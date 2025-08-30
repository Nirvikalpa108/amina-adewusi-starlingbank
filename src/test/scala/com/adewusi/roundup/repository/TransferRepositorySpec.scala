package com.adewusi.roundup.repository

import cats.effect.{IO, Ref}
import cats.implicits._
import com.adewusi.roundup.model.{AddMoneyResponse, TransferRecord}
import munit.CatsEffectSuite

import java.time.LocalDate
import java.util.UUID

class TransferRepositorySpec extends CatsEffectSuite {

  val testGoal: UUID = UUID.randomUUID()
  val testGoal2: UUID = UUID.randomUUID()
  val testDate: LocalDate = LocalDate.of(2025, 8, 25) // Monday
  val testAmount: Long = 158L // £1.58 in pence
  val testTransferUid: UUID = UUID.randomUUID()
  val testResponse: AddMoneyResponse =
    AddMoneyResponse(testTransferUid, success = true)

  private def withRepo[A](initial: Set[TransferRecord] = Set.empty)(test: TransferRepository[IO] => IO[A]): IO[A] =
    Ref.of[IO, Set[TransferRecord]](initial)
      .map(TransferRepository.inMemoryTransferRepository[IO])
      .flatMap(test)

  test("isEligibleForTransfer should return true when no transfers exist") {
    withRepo() { repo =>
      repo.isEligibleForTransfer(testGoal, testDate, testAmount).assertEquals(Right(true))
    }
  }

  test("eligibility should return false for any day in the 7-day range") {
    val existing = TransferRecord(testGoal, testDate, testAmount, testResponse.transferUid)
    withRepo(Set(existing)) { repo =>
      (0 to 6).toList.traverse_ { dayOffset =>
        repo.isEligibleForTransfer(testGoal, testDate.plusDays(dayOffset.toLong), testAmount).flatMap { r =>
          IO(assertEquals(r, Right(false), s"Failed on day offset $dayOffset"))
        }
      }
    }
  }

  test("eligibility should return true for different goals in same week") {
    val existing = TransferRecord(testGoal, testDate, testAmount, testResponse.transferUid)
    withRepo(Set(existing)) { repo =>
      repo.isEligibleForTransfer(testGoal2, testDate, testAmount).assertEquals(Right(true))
    }
  }

  test("eligibility should return true for same goal in different week") {
    val existing = TransferRecord(testGoal, testDate, testAmount, testResponse.transferUid)
    withRepo(Set(existing)) { repo =>
      repo.isEligibleForTransfer(testGoal, testDate.plusDays(8), testAmount).assertEquals(Right(true))
    }
  }

  test("eligibility should ignore roundup amount when checking duplicates") {
    val existing = TransferRecord(testGoal, testDate, 158L, testResponse.transferUid)
    withRepo(Set(existing)) { repo =>
      repo.isEligibleForTransfer(testGoal, testDate, 250L).assertEquals(Right(false))
    }
  }

  test("eligibility boundary: day 7 should be eligible (outside 7-day window)") {
    val existing = TransferRecord(testGoal, testDate, testAmount, testResponse.transferUid)
    withRepo(Set(existing)) { repo =>
      repo.isEligibleForTransfer(testGoal, testDate.plusDays(7), testAmount).assertEquals(Right(true))
    }
  }

  test("recordTransfer should successfully record a transfer") {
    withRepo() { repo =>
      for {
        before <- repo.isEligibleForTransfer(testGoal, testDate, testAmount)
        result <- repo.recordTransfer(testGoal, testDate, testAmount, testResponse)
        after <- repo.isEligibleForTransfer(testGoal, testDate, testAmount)
      } yield {
        assertEquals(before, Right(true))
        assertEquals(result, Right(()))
        assertEquals(after, Right(false))
      }
    }
  }

  test("recordTransfer should allow recording transfers for different goals") {
    withRepo() { repo =>
      for {
        result1 <- repo.recordTransfer(testGoal, testDate, testAmount, testResponse)
        result2 <- repo.recordTransfer(testGoal2, testDate, testAmount, testResponse)
      } yield {
        assertEquals(result1, Right(()))
        assertEquals(result2, Right(()))
      }
    }
  }

  test("recordTransfer should allow recording transfers for different weeks") {
    withRepo() { repo =>
      for {
        result1 <- repo.recordTransfer(testGoal, testDate, testAmount, testResponse)
        result2 <- repo.recordTransfer(testGoal, testDate.plusDays(8), testAmount, testResponse)
      } yield {
        assertEquals(result1, Right(()))
        assertEquals(result2, Right(()))
      }
    }
  }
}