package com.adewusi.roundup.it

import cats.effect.IO
import com.adewusi.roundup.RoundupApp
import munit.CatsEffectSuite
import cats.syntax.all._
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RoundupAppIntegrationTest extends CatsEffectSuite with IntegrationTestUtils {

  test("RoundupApp.run creates goal and transfers, then cleans up") {
    setupResources.use { client =>
      setupApp(client, goalInitialState = None, transferRepoInitialState = Set.empty).flatMap { appContext =>
        val goalRef = appContext.goalRef
        val transferRef = appContext.transferRef
        val config = appContext.config
        val accountUuid = appContext.accountUuid

        val startDate: LocalDate = config.starling.startDate.map(dateStr =>
          LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE))
          .getOrElse(LocalDate.now())

        for {
          goalValue <- goalRef.get
          _ <- IO.println(s"Goal ref BEFORE test: $goalValue")
          transfersValue <- transferRef.get
          _ <- IO.println(s"Transfers BEFORE test: $transfersValue")

          result <- RoundupApp.run(startDate, isDryRun = false, goalRef, transferRef)
          _ = assert(result.isRight, "First run should succeed")
          _ <- IO.println(s"Result: $result")

          // Capture the goal and transfer state after first run
          goalAfterFirstRun <- goalRef.get
          transfersAfterFirstRun <- transferRef.get

          _ = assert(goalAfterFirstRun.isDefined, "Goal should be created on first run")
          _ <- IO.println(s"Goal ref AFTER: $goalAfterFirstRun")
          _ = assert(transfersAfterFirstRun.nonEmpty, "Transfers should be recorded on first run")
          _ <- IO.println(s"Transfer ref AFTER: $transfersAfterFirstRun")

          // Cleanup: delete the savings goal from Starling API
          _ <- goalAfterFirstRun.traverse_ { goalId =>
            deleteSavingsGoal(config.starling.baseUri, client, config.starling.accessToken, accountUuid, goalId)
          }
        } yield ()
      }
    }
  }
}
