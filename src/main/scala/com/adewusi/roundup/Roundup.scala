package com.adewusi.roundup

import cats.data.EitherT
import cats.effect.Sync
import com.adewusi.roundup.model._

import java.time.LocalDate
import java.util.UUID

object Roundup {

  def processRoundups[F[_]: Sync](
      startDate: LocalDate,
      config: AppConfig
  )(implicit
      accountClient: AccountClient[F],
      transactionClient: TransactionClient[F],
      savingsGoalClient: SavingsGoalClient[F],
      idempotencyService: IdempotencyClient[F],
      accountSelector: AccountSelector,
      transactionValidator: TransactionValidator
  ): EitherT[F, AppError, Unit] = {
    for {
      accounts <- EitherT(accountClient.fetchAccounts)
      account <- EitherT.fromEither(accountSelector.getCorrectAccount(accounts))
      transactions <- EitherT(transactionClient.fetchTransactions(account, startDate))
      validatedTransactions <- EitherT.fromEither(transactionValidator.validateTransactions(transactions))
      roundup <- EitherT.fromEither(transactionValidator.validateRoundupAmount(validatedTransactions))
      goalUuid <- EitherT(savingsGoalClient.readGoalFromFile(config)).flatMap {
        case Some(goal) =>
          EitherT(savingsGoalClient.validateGoal(goal, account.accountUid)).map(_.savingsGoalUid)
        case None =>
          EitherT(savingsGoalClient.createAndPersistGoal(config, account.accountUid))
      }
      // NOTE: In production, idempotency + recording should be atomic to avoid race conditions.
      // This test assumes single-instance execution.
      needsProcessing <- EitherT(
        idempotencyService.checkIdempotency(goalUuid, startDate, roundup)
      )
      _ <- EitherT.cond[F](
        needsProcessing,
        (),
        AlreadyTransferred(s"Roundup for $startDate already processed")
      )
      transfer <- EitherT(savingsGoalClient.transferToSavingsGoal(config, account.accountUid, goalUuid, roundup))
      _ <- EitherT(
        idempotencyService.recordTransfer(goalUuid, startDate, roundup, transfer)
      )
    } yield ()
  }
}

trait IdempotencyClient[F[_]] {
  def checkIdempotency(
      goal: UUID,
      startDate: LocalDate,
      amountMinorUnits: Long
  ): F[Either[AppError, Boolean]]
  def recordTransfer(
      goal: UUID,
      startDate: LocalDate,
      amountMinorUnits: Long,
      transfer: AddMoneyResponse
  ): F[Either[AppError, Unit]]
}

