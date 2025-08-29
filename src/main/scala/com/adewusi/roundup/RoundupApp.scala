package com.adewusi.roundup

import cats.effect.{IO, Ref}
import com.adewusi.roundup.clients.{AccountClient, SavingsGoalClient, TransactionClient}
import com.adewusi.roundup.domain.{AccountSelector, TransactionValidator}
import com.adewusi.roundup.model.{AppError, RoundupResult, TransferRecord}
import com.adewusi.roundup.repository.{GoalRepository, TransferRepository}
import com.adewusi.roundup.services.{GoalService, RoundupService}
import com.adewusi.roundup.starlingapis.{StarlingAccountsApi, StarlingSavingsGoalsApi, StarlingTransactionApi}
import org.http4s.ember.client.EmberClientBuilder

import java.time.LocalDate
import java.util.UUID

object RoundupApp {

  def run(startDate: LocalDate, isDryRun: Boolean, goalRepoRef: Ref[IO, Option[UUID]], transferRepoRef: Ref[IO, Set[TransferRecord]]): IO[Either[AppError, RoundupResult]] =
    Config.load.flatMap { config =>
      EmberClientBuilder.default[IO].build.use { client =>
        for {
          result <- {
            implicit val starlingAccountsApi: StarlingAccountsApi[IO] = StarlingAccountsApi.impl[IO](client)
            implicit val starlingTransactionApi: StarlingTransactionApi[IO] = StarlingTransactionApi.impl[IO](client, config)
            implicit val starlingSavingsApi: StarlingSavingsGoalsApi[IO] = StarlingSavingsGoalsApi.impl[IO](client, config)
            implicit val accountClient: AccountClient[IO] = AccountClient.impl[IO](config)
            implicit val accountSelector: AccountSelector = AccountSelector.impl
            implicit val transactionClient: TransactionClient[IO] = TransactionClient.impl[IO]
            implicit val transactionValidator: TransactionValidator = TransactionValidator.impl
            implicit val savingsGoalClient: SavingsGoalClient[IO] = if (isDryRun) SavingsGoalClient.dryRun[IO] else SavingsGoalClient.impl[IO]
            implicit val transferRepository: TransferRepository[IO] = if (isDryRun) TransferRepository.dryRun[IO](transferRepoRef) else TransferRepository.inMemoryTransferRepository[IO](transferRepoRef)
            implicit val goalRepository: GoalRepository[IO] = if (isDryRun) GoalRepository.dryRun[IO](goalRepoRef) else GoalRepository.inMemoryGoalRepository[IO](goalRepoRef)
            implicit val goalService: GoalService[IO] = if (isDryRun) GoalService.dryRun[IO] else GoalService.impl[IO]

            RoundupService
              .processRoundups[IO](startDate = startDate, config)
              .value
          }
        } yield result
      }
    }
}
