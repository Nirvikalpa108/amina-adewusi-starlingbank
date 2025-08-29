//import cats.effect.{IO, Ref}
//import com.adewusi.roundup.RoundupApp
//import com.adewusi.roundup.model.TransferRecord
//import munit.CatsEffectSuite
//
//import java.time.LocalDate
//import java.util.UUID
//
//class RoundupAppDryRunSpec extends CatsEffectSuite {
//
//  test("RoundupApp.run with dryRun=true does not modify state and returns expected result") {
//    val startDate = LocalDate.of(2025, 8, 29)
//    val initialGoalId = UUID.randomUUID()
//    val initialTransfers = Set.empty[TransferRecord]
//
//    for {
//      goalRepoRef <- Ref.of[IO, Option[UUID]](Some(initialGoalId))
//      transferRepoRef <- Ref.of[IO, Set[TransferRecord]](initialTransfers)
//
//      result <- RoundupApp.run(startDate, isDryRun = true)
//
//      // Check that the refs are unchanged
//      finalGoalId <- goalRepoRef.get
//      finalTransfers <- transferRepoRef.get
//    } yield {
//      assertEquals(finalGoalId, Some(initialGoalId), "GoalRef should be unchanged in dry run")
//      assertEquals(finalTransfers, initialTransfers, "TransferRef should be unchanged in dry run")
//      println(s"RoundupApp.run result: $result")
//      assert(result.isRight, s"Dry run failed with error: $result")
//    }
//  }
//}