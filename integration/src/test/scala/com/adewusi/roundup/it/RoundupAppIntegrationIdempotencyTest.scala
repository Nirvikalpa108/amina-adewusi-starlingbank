package com.adewusi.roundup.it

import cats.effect.IO
import cats.syntax.all._
import com.adewusi.roundup.RoundupApp
import munit.CatsEffectSuite

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RoundupAppIntegrationIdempotencyTest extends CatsEffectSuite with IntegrationTestUtils {


  test("RoundupApp.run reuses goal and errors on duplicate transfer") {
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

          // First run: should succeed and populate refs
          firstResult <- RoundupApp.run(startDate, isDryRun = false, goalRef, transferRef)
          _ = assert(firstResult.isRight, s"First run should succeed but got $firstResult")
          _ <- IO.println(s"First run result: $firstResult")

          goalAfterFirstRun <- goalRef.get
          transfersAfterFirstRun <- transferRef.get

          _ = assert(goalAfterFirstRun.isDefined, "Goal should be created on first run")
          _ <- IO.println(s"Goal ref AFTER: $goalAfterFirstRun")

          _ = assert(transfersAfterFirstRun.nonEmpty, "Transfers should be recorded on first run")
          _ <- IO.println(s"Transfer ref AFTER: $transfersAfterFirstRun")

          // Capture the goal UUID for comparison
          goalId = goalAfterFirstRun.get

          // Second run: should reuse the same goal and error due to duplicate transfer
          secondResult <- RoundupApp.run(startDate, isDryRun = false, goalRef, transferRef)
          _ <- IO.println(s"Second run result: $secondResult")

          _ = assert(secondResult.isLeft, s"Second run should fail due to duplicate transfer but got $secondResult")

          // Verify the goal UUID is unchanged
          goalAfterSecondRun <- goalRef.get
          _ = assert(goalAfterSecondRun.contains(goalId), "Goal UUID should be the same after second run")

          _ <- goalAfterSecondRun.traverse_ { goalId =>
            deleteSavingsGoal(config.starling.baseUrl, client, config.starling.accessToken, accountUuid, goalId)
          }

          _ <- goalAfterFirstRun.traverse_ { goalId =>
            deleteSavingsGoal(config.starling.baseUrl, client, config.starling.accessToken, accountUuid, goalId)
          }
        } yield ()
      }
    }
  }
}
