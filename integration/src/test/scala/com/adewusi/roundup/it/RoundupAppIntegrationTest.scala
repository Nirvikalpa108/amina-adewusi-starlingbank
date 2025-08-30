package com.adewusi.roundup.it

import cats.effect.IO
import cats.syntax.all._
import com.adewusi.roundup.{Config, RoundupApp}
import munit.CatsEffectSuite

import java.time.LocalDate

class RoundupAppIntegrationTest extends CatsEffectSuite with IntegrationTestUtils {

  test("RoundupApp.run creates goal and transfers, then cleans up") {
    for {
      config <- Config.load
      _ <- setupResources.use { client =>
        setupApp(client, goalInitialState = None, transferRepoInitialState = Set.empty).flatMap { appContext =>
          val goalRef = appContext.goalRef
          val transferRef = appContext.transferRef
          val accountUuid = appContext.accountUuid

          val startDate: LocalDate = LocalDate.now()

          for {
            goalValue <- goalRef.get
            _ <- IO.println(s"Goal ref BEFORE test: $goalValue")
            transfersValue <- transferRef.get
            _ <- IO.println(s"Transfers BEFORE test: $transfersValue")

            result <- RoundupApp.run(startDate, isDryRun = false, goalRepoRef = goalRef, transferRepoRef = transferRef, goalId = None, config = config)
            _ = assert(result.isRight, "First run should succeed")
            _ <- IO.println(s"Result: $result")

            goalAfterFirstRun <- goalRef.get
            transfersAfterFirstRun <- transferRef.get

            _ = assert(goalAfterFirstRun.isDefined, "Goal should be created on first run")
            _ <- IO.println(s"Goal ref AFTER: $goalAfterFirstRun")
            _ = assert(transfersAfterFirstRun.nonEmpty, "Transfers should be recorded on first run")
            _ <- IO.println(s"Transfer ref AFTER: $transfersAfterFirstRun")

            _ <- goalAfterFirstRun.traverse_ { goalId =>
              deleteSavingsGoal(config.starling.baseUri, client, config.starling.accessToken, accountUuid, goalId)
            }
          } yield ()
        }
      }
    } yield ()
  }
}
