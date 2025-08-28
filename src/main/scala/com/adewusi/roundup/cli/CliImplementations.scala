package com.adewusi.roundup.cli

import cats.Monad
import cats.effect.{Concurrent, Sync}
import com.adewusi.roundup.clients.{AccountClient, SavingsGoalClient, TransactionClient}
import com.adewusi.roundup.domain.{AccountSelector, TransactionValidator}
import com.adewusi.roundup.model.AppConfig
import com.adewusi.roundup.repository._
import com.adewusi.roundup.services.GoalService
import com.adewusi.roundup.starlingapis.{StarlingAccountsApi, StarlingTransactionApi}

object RoundupImplicits {

  def make[F[_]: Concurrent: Sync: Monad](config: AppConfig, isDryRun: Boolean)(
    implicit
    starlingAccountsApi: StarlingAccountsApi[F],
    starlingTransactionApi: StarlingTransactionApi[F]
  ) = new {

    implicit val accountClient: AccountClient[F] = AccountClient.impl[F]
    implicit val transactionClient: TransactionClient[F] = TransactionClient.impl[F]
    implicit val accountSelector: AccountSelector = AccountSelector.impl
    implicit val transactionValidator: TransactionValidator = TransactionValidator.impl

    implicit val savingsGoalClient: SavingsGoalClient[F] =
      if (isDryRun) SavingsGoalClient.dryRun[F]
      else SavingsGoalClient.impl[F]

    implicit val transferRepository: TransferRepository[F] =
      if (isDryRun) TransferRepository.dryRun[F]
      else TransferRepository.impl[F](config)

    implicit val goalRepository: GoalRepository[F] =
      if (isDryRun) GoalRepository.inMemory[F]
      else GoalRepository.impl[F](config)

    implicit val goalService: GoalService[F] =
      GoalService.impl[F](goalRepository, savingsGoalClient)
  }
}