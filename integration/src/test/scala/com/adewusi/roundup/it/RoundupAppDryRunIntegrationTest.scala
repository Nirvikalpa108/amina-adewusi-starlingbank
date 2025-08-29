package com.adewusi.roundup.it

import cats.effect.{IO, Ref}
import com.adewusi.roundup.{RoundupApp, Config}
import com.adewusi.roundup.model.TransferRecord
import munit.CatsEffectSuite

import java.time.LocalDate
import java.util.UUID

class RoundupAppDryRunIntegrationTest extends CatsEffectSuite {

  test("RoundupApp.run with dryRun=true does not modify state and returns expected result") {
    for {
      config <- Config.load
      startDate = config.starling.startDate.map(LocalDate.parse).getOrElse(LocalDate.now())  // fallback to today

      initialGoalId = UUID.randomUUID()
      initialTransfers = Set.empty[TransferRecord]

      goalRef <- Ref.of[IO, Option[UUID]](Some(initialGoalId))
      transferRef <- Ref.of[IO, Set[TransferRecord]](initialTransfers)

      result <- RoundupApp.run(startDate, isDryRun = true, goalRepoRef = goalRef, transferRepoRef = transferRef)

      finalGoalId <- goalRef.get
      finalTransfers <- transferRef.get
    } yield {
      assertEquals(finalGoalId, Some(initialGoalId), "GoalRef should be unchanged in dry run")
      assertEquals(finalTransfers, initialTransfers, "TransferRef should be unchanged in dry run")
      println(s"RoundupApp.run result: $result")
      assert(result.isRight, s"Dry run failed with error: $result")
    }
  }
}