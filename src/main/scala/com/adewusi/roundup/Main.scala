package com.adewusi.roundup

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.adewusi.roundup.cli.Cli._
import com.adewusi.roundup.model.AppConfig
import com.adewusi.roundup.starlingapis.{StarlingAccountsApi, StarlingTransactionApi}

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    parseArgs(args) match {
      case Left(err) =>
        IO.println(s"❌ $err") *> printUsage.as(ExitCode.Error)

      case Right(cliArgs) =>
        Config.load.flatMap { config: AppConfig =>
          implicit val starlingAccountsApi: StarlingAccountsApi[IO] = StarlingAccountsApi.impl[IO](client = ???, config = config)
          implicit val starlingTransactionApi: StarlingTransactionApi[IO] = StarlingTransactionApi.impl[IO](client = ???, config = config)
          val mode = if (cliArgs.dryRun) "🔍 Dry run" else "🚀 Live run"
          IO.println(s"$mode starting from ${cliArgs.startDate}") *> {
            import com.adewusi.roundup.cli.RoundupImplicits.make(config, cliArgs.dryRun)._
            Roundup.processRoundups[IO](cliArgs.startDate, config).value.flatMap {
              case Left(e) => IO.println(s"Error: $e")
              case Right(_) => IO.println("✅ Done")
            }
          }.as(ExitCode.Success)
        }
    }
}

//object Main extends IOApp.Simple {
//  val run: IO[Unit] = Config.load.flatMap(config => RoundupServer.run[IO](config))
//}

//object Main extends IOApp.Simple {
//
//  val run: IO[Unit] =
//    load.flatMap { config =>
//      EmberClientBuilder.default[IO].build.use { httpClient =>
//        val starlingAccountsApi = StarlingAccountsApi.impl[IO](httpClient, config)
//        implicit val accountClient: AccountClient[IO] = AccountClient.impl(starlingAccountsApi)
//        implicit val goalRepo: GoalRepository[IO] = GoalRepository.inMemoryGoalRepository[IO](config)
//
//        val starlingTxApi     = ...
//        implicit val txClient = TransactionClient.impl(starlingTxApi)
//
//        Roundup.processRoundups[IO](startDate = ???, config, savingsGoalId = None).value.flatMap {
//          case Left(err)  => IO.println(s"Error: $err")
//          case Right(())  => IO.println("Roundup processed successfully!")
//        }
//      }
//    }
//config <- Config.load
//
//    // allocate in-memory Ref with optional initial GoalId from config
//    goalRef <- Ref.of[IO, Option[UUID]](config.starling.initialGoalId)

//def processRoundups[F[_]: Sync](
//      startDate: LocalDate,
//      config: AppConfig
//  )(implicit
//    accountClient: AccountClient[F],
//    transactionClient: TransactionClient[F],
//    savingsGoalClient: SavingsGoalClient[F],
//    goalRepository: GoalRepository[F],
//    goalService: GoalService[F],
//    transferLedger: TransferRepository[F],
//    accountSelector: AccountSelector,
//    transactionValidator: TransactionValidator
//  ): EitherT[F, AppError, Unit] = {


