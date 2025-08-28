package com.adewusi.roundup.repository

import cats.implicits._
import cats.effect.{IO, Ref}
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

  private def createRepoWithRef(
      ref: Ref[IO, Set[TransferRecord]]
  ): TransferRepository[IO] =
    TransferRepository.inMemoryTransferRepository[IO](ref)

  test("isEligibleForTransfer should return true when no transfers exist") {
    for {
      ref <- Ref.of[IO, Set[TransferRecord]](Set.empty)
      repo = createRepoWithRef(ref)
      result <- repo.isEligibleForTransfer(testGoal, testDate, testAmount)
    } yield {
      assertEquals(result, Right(true))
    }
  }

  test("eligibility should return false for any day in the 7‑day range") {
    val existing = TransferRecord(testGoal, testDate, testAmount, testResponse.transferUid)
    for {
      ref <- Ref.of[IO, Set[TransferRecord]](Set(existing))
      repo = createRepoWithRef(ref)
      results <- (0 to 6).toList.traverse { dayOffset =>
        repo.isEligibleForTransfer(testGoal, testDate.plusDays(dayOffset.toLong), testAmount)
      }
    } yield results.foreach(r => assertEquals(r, Right(false)))
  }

  test("eligibility should return true for different goals in same week") {
    val existing = TransferRecord(testGoal, testDate, testAmount, testResponse.transferUid)
    for {
      ref <- Ref.of[IO, Set[TransferRecord]](Set(existing))
      repo = createRepoWithRef(ref)
      result <- repo.isEligibleForTransfer(testGoal2, testDate, testAmount)
    } yield assertEquals(result, Right(true))
  }

  test("eligibility should return true for same goal in different week") {
    val existing = TransferRecord(testGoal, testDate, testAmount, testResponse.transferUid)
    for {
      ref <- Ref.of[IO, Set[TransferRecord]](Set(existing))
      repo = createRepoWithRef(ref)
      result <- repo.isEligibleForTransfer(testGoal, testDate.plusDays(8), testAmount)
    } yield assertEquals(result, Right(true))
  }

  test("eligibility should ignore roundup amount when checking duplicates") {
    val existing = TransferRecord(testGoal, testDate, 158L, testResponse.transferUid)
    for {
      ref <- Ref.of[IO, Set[TransferRecord]](Set(existing))
      repo = createRepoWithRef(ref)
      result <- repo.isEligibleForTransfer(testGoal, testDate, 250L)
    } yield assertEquals(result, Right(false))
  }

  test("eligibility boundary: day 7 should be eligible (outside 7‑day window)") {
    val existing = TransferRecord(testGoal, testDate, testAmount, testResponse.transferUid)
    for {
      ref <- Ref.of[IO, Set[TransferRecord]](Set(existing))
      repo = createRepoWithRef(ref)
      result <- repo.isEligibleForTransfer(testGoal, testDate.plusDays(7), testAmount)
    } yield assertEquals(result, Right(true))
  }

  test("recordTransfer should successfully record a transfer") {
    for {
      ref <- Ref.of[IO, Set[TransferRecord]](Set.empty)
      repo = createRepoWithRef(ref)
      result <- repo.recordTransfer(
        testGoal,
        testDate,
        testAmount,
        testResponse
      )
    } yield {
      assertEquals(result, Right(()))
    }
  }

  test("recordTransfer should allow recording transfers for different goals") {
    for {
      ref <- Ref.of[IO, Set[TransferRecord]](Set.empty)
      repo = createRepoWithRef(ref)
      result1 <- repo.recordTransfer(
        testGoal,
        testDate,
        testAmount,
        testResponse
      )
      result2 <- repo.recordTransfer(
        testGoal2,
        testDate,
        testAmount,
        testResponse
      )
    } yield {
      assertEquals(result1, Right(()))
      assertEquals(result2, Right(()))
    }
  }

  test("recordTransfer should allow recording transfers for different weeks") {
    for {
      ref <- Ref.of[IO, Set[TransferRecord]](Set.empty)
      repo = createRepoWithRef(ref)
      result1 <- repo.recordTransfer(
        testGoal,
        testDate,
        testAmount,
        testResponse
      )
      result2 <- repo.recordTransfer(
        testGoal,
        testDate.plusDays(8),
        testAmount,
        testResponse
      )
    } yield {
      assertEquals(result1, Right(()))
      assertEquals(result2, Right(()))
    }
  }
}
