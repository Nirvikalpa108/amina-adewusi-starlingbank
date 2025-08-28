package com.adewusi.roundup.repository

import cats.implicits._
import cats.effect.IO
import com.adewusi.roundup.model.AddMoneyResponse
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

  test("isEligibleForTransfer should return true when no transfers exist") {
    for {
      repo <- TransferRepository.inMemoryTransferRepository[IO]
      result <- repo.isEligibleForTransfer(testGoal, testDate, testAmount)
    } yield {
      assertEquals(result, Right(true))
    }
  }

  test(
    "isEligibleForTransfer should return false when transfer already exists for same goal and week"
  ) {
    for {
      repo <- TransferRepository.inMemoryTransferRepository[IO]
      _ <- repo.recordTransfer(testGoal, testDate, testAmount, testResponse)
      result <- repo.isEligibleForTransfer(testGoal, testDate, testAmount)
    } yield {
      assertEquals(result, Right(false))
    }
  }

  test(
    "isEligibleForTransfer should return false when transfer exists within the 7-day window"
  ) {
    for {
      repo <- TransferRepository.inMemoryTransferRepository[IO]
      // Record transfer on Monday
      _ <- repo.recordTransfer(testGoal, testDate, testAmount, testResponse)
      // Check eligibility on Wednesday (within same week)
      result <- repo.isEligibleForTransfer(
        testGoal,
        testDate.plusDays(2),
        testAmount
      )
    } yield {
      assertEquals(result, Right(false))
    }
  }

  test(
    "isEligibleForTransfer should return false for any day in the 7-day range"
  ) {
    for {
      repo <- TransferRepository.inMemoryTransferRepository[IO]
      // Record transfer on Monday
      _ <- repo.recordTransfer(testGoal, testDate, testAmount, testResponse)
      // Test each day of the week (0-6 days after start)
      results <- (0 to 6).toList.traverse { dayOffset =>
        repo.isEligibleForTransfer(
          testGoal,
          testDate.plusDays(dayOffset.toLong),
          testAmount
        )
      }
    } yield {
      // All days in the week should be ineligible
      results.foreach(result => assertEquals(result, Right(false)))
    }
  }

  test(
    "isEligibleForTransfer should return true for different goals in same week"
  ) {
    for {
      repo <- TransferRepository.inMemoryTransferRepository[IO]
      // Record transfer for goal 1
      _ <- repo.recordTransfer(testGoal, testDate, testAmount, testResponse)
      // Check eligibility for goal 2 in same week
      result <- repo.isEligibleForTransfer(testGoal2, testDate, testAmount)
    } yield {
      assertEquals(result, Right(true))
    }
  }

  test(
    "isEligibleForTransfer should return true for same goal in different week"
  ) {
    for {
      repo <- TransferRepository.inMemoryTransferRepository[IO]
      // Record transfer for week 1
      _ <- repo.recordTransfer(testGoal, testDate, testAmount, testResponse)
      // Check eligibility for week 2 (8 days later, outside the 7-day window)
      result <- repo.isEligibleForTransfer(
        testGoal,
        testDate.plusDays(8),
        testAmount
      )
    } yield {
      assertEquals(result, Right(true))
    }
  }

  test(
    "isEligibleForTransfer should ignore roundup amount when checking duplicates"
  ) {
    for {
      repo <- TransferRepository.inMemoryTransferRepository[IO]
      // Record transfer with amount 158
      _ <- repo.recordTransfer(testGoal, testDate, 158L, testResponse)
      // Check eligibility with different amount (250) but same goal and week
      result <- repo.isEligibleForTransfer(testGoal, testDate, 250L)
    } yield {
      assertEquals(result, Right(false))
    }
  }

  test("recordTransfer should successfully record a transfer") {
    for {
      repo <- TransferRepository.inMemoryTransferRepository[IO]
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
      repo <- TransferRepository.inMemoryTransferRepository[IO]
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
      repo <- TransferRepository.inMemoryTransferRepository[IO]
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

  test("boundary test: day 7 should be eligible (outside 7-day window)") {
    for {
      repo <- TransferRepository.inMemoryTransferRepository[IO]
      // Record transfer on Monday
      _ <- repo.recordTransfer(testGoal, testDate, testAmount, testResponse)
      // Check eligibility exactly 7 days later (should be eligible)
      result <- repo.isEligibleForTransfer(
        testGoal,
        testDate.plusDays(7),
        testAmount
      )
    } yield {
      assertEquals(result, Right(true))
    }
  }
}
