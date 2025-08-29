import cats.effect.{IO, Ref}
import com.adewusi.roundup.RoundupApp
import com.adewusi.roundup.model.TransferRecord
import munit.CatsEffectSuite

import java.time.LocalDate
import java.util.UUID

class RoundupAppDryRunSpec extends CatsEffectSuite {

  test("RoundupApp.run with dryRun=true does not modify state and returns expected result") {
    val startDate = LocalDate.of(2025, 8, 29)
    val initialGoalId = UUID.randomUUID()
    val initialTransfers = Set.empty[TransferRecord]

    for {
      goalRef <- Ref.of[IO, Option[UUID]](Some(initialGoalId))
      transferRef <- Ref.of[IO, Set[TransferRecord]](initialTransfers)

      result <- RoundupApp.run(startDate, isDryRun = true, goalRepoRef = goalRef, transferRepoRef = transferRef)

      // Check that the refs are unchanged
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