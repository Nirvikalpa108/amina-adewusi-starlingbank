package com.adewusi.roundup

import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple {
  val run: IO[Unit] = Config.load.flatMap(config => RoundupServer.run[IO](config))
}

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

