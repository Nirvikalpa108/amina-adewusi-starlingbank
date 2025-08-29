package com.adewusi.roundup.services

import cats.data.EitherT
import cats.effect.Sync
import com.adewusi.roundup.clients.{
  AccountClient,
  SavingsGoalClient,
  TransactionClient
}
import com.adewusi.roundup.domain.{AccountSelector, TransactionValidator}
import com.adewusi.roundup.model.{AlreadyTransferred, AppConfig, AppError}
import com.adewusi.roundup.repository.{GoalRepository, TransferRepository}

import java.time.LocalDate

object RoundupService {

  def processRoundups[F[_]: Sync](
      startDate: LocalDate,
      config: AppConfig
  )(implicit
      accountClient: AccountClient[F],
      transactionClient: TransactionClient[F],
      savingsGoalClient: SavingsGoalClient[F],
      goalRepository: GoalRepository[F],
      goalService: GoalService[F],
      transferLedger: TransferRepository[F],
      accountSelector: AccountSelector,
      transactionValidator: TransactionValidator
  ): EitherT[F, AppError, Unit] = {
    for {
      accounts <- EitherT(accountClient.fetchAccounts)
      account <- EitherT.fromEither(accountSelector.getCorrectAccount(accounts))
      transactions <- EitherT(
        transactionClient.fetchTransactions(account, startDate)
      )
      validatedTransactions <- EitherT.fromEither(
        transactionValidator.validateTransactions(
          transactions,
          account.defaultCategory
        )
      )
      roundup <- EitherT.fromEither(
        transactionValidator.validateRoundupAmount(
          validatedTransactions,
          account.defaultCategory
        )
      )
      goalUuid <- EitherT(
        goalService.getOrCreateGoal(config, account.accountUid)
      )
      _ <- EitherT(goalRepository.persistGoal(goalUuid))
      needsProcessing <- EitherT(
        transferLedger.isEligibleForTransfer(goalUuid, startDate, roundup)
      )
      _ <- EitherT.cond[F](
        needsProcessing,
        (),
        AlreadyTransferred(s"Roundup for $startDate already processed")
      )
      transfer <- EitherT(
        savingsGoalClient.transferToGoal(account.accountUid, goalUuid, roundup)
      )
      _ <- EitherT(
        transferLedger.recordTransfer(goalUuid, startDate, roundup, transfer)
      )
    } yield ()
  }
}
