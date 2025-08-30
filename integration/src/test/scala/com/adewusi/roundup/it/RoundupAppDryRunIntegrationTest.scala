package com.adewusi.roundup.it

import cats.effect.{IO, Ref}
import com.adewusi.roundup.{Config, RoundupApp}
import com.adewusi.roundup.model.TransferRecord
import munit.CatsEffectSuite

import java.time.LocalDate
import java.util.UUID

class RoundupAppDryRunIntegrationTest extends CatsEffectSuite {

  test("RoundupApp.run with dryRun=true does not modify state and returns expected result") {
    val initialGoalId = UUID.randomUUID()
    val initialTransfers = Set.empty[TransferRecord]

    for {
      config <- Config.load

      goalRef <- Ref.of[IO, Option[UUID]](Some(initialGoalId))
      transferRef <- Ref.of[IO, Set[TransferRecord]](initialTransfers)

      result <- RoundupApp.run(
        startDate = LocalDate.now(),
        isDryRun = true,
        goalRepoRef = goalRef,
        transferRepoRef = transferRef,
        goalId = None,
        config = config
      )

      finalGoalId <- goalRef.get
      finalTransfers <- transferRef.get
      _ <- IO.println(s"RoundupApp.run result: $result")
    } yield {
      assertEquals(
        finalGoalId,
        Some(initialGoalId),
        "GoalRef should be unchanged in dry run"
      )
      assertEquals(
        finalTransfers,
        initialTransfers,
        "TransferRef should be unchanged in dry run"
      )
      assert(result.isRight, s"Dry run failed with error: $result")
    }
  }
}