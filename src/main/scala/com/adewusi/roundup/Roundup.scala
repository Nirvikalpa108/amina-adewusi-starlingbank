package com.adewusi.roundup

import cats.data.EitherT
import cats.effect.Sync
import com.adewusi.roundup.model._
import java.time.LocalDate
import java.util.UUID

object Roundup {

  def processRoundups[F[_]: Sync](
      startDate: LocalDate,
      config: AppConfig,
      savingsGoalId: Option[UUID]
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
      goal <- EitherT(savingsGoalClient.fetchOrCreateSavingsGoal(config, savingsGoalId))
      // NOTE: In production, idempotency + recording should be atomic to avoid race conditions.
      // This test assumes single-instance execution.
      needsProcessing <- EitherT(
        idempotencyService.checkIdempotency(goal, startDate, roundup)
      )
      _ <- EitherT.cond[F](
        needsProcessing,
        (),
        AlreadyTransferred(s"Roundup for $startDate already processed")
      )
      transfer <- EitherT(savingsGoalClient.transferToSavingsGoal(config, goal, roundup))
      _ <- EitherT(
        idempotencyService.recordTransfer(goal, startDate, roundup, transfer)
      )
    } yield ()
  }
}

trait IdempotencyClient[F[_]] {
  def checkIdempotency(
      goal: SavingsGoal,
      startDate: LocalDate,
      amountMinorUnits: Long
  ): F[Either[AppError, Boolean]]
  def recordTransfer(
      goal: SavingsGoal,
      startDate: LocalDate,
      amountMinorUnits: Long,
      transfer: AddMoneyResponse
  ): F[Either[AppError, Unit]]
}

