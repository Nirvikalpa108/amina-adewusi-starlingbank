package com.adewusi.roundup.services

import cats.effect.IO
import com.adewusi.roundup.clients.SavingsGoalClient
import com.adewusi.roundup.model._
import com.adewusi.roundup.repository.GoalRepository
import munit.CatsEffectSuite
import java.util.UUID

class GoalServiceSpec extends CatsEffectSuite {

  val testAccountId: UUID = UUID.randomUUID()
  val testGoalId: UUID = UUID.randomUUID()
  val testConfig: AppConfig = AppConfig(
    StarlingConfig("token", "https://api-sandbox.starlingbank.com", None)
  )

  private def createTestSavingsGoal(goalId: UUID): SavingsGoal = {
    SavingsGoal(
      savingsGoalUid = goalId,
      name = "Test Roundup Goal",
      target = None,
      totalSaved = CurrencyAndAmount("GBP", 0),
      savedPercentage = Some(0),
      state = "ACTIVE"
    )
  }

  /** Helper to create a GoalService with easily configurable behaviour.
    */
  private def mkService(
      readGoalResult: Either[AppError, Option[UUID]],
      persistGoalResult: Either[AppError, Unit] = Right(()),
      getGoalResult: Either[AppError, SavingsGoal] = Left(
        GenericError("getGoal not set")
      ),
      createGoalResult: Either[AppError, UUID] = Left(
        GenericError("createGoal not set")
      )
  ): GoalService[IO] = {

    implicit val repo: GoalRepository[IO] = new GoalRepository[IO] {
      def readGoal(config: AppConfig): IO[Either[AppError, Option[UUID]]] =
        IO.pure(readGoalResult)
      def persistGoal(goalId: UUID): IO[Either[AppError, Unit]] =
        IO.pure(persistGoalResult)
    }

    implicit val client: SavingsGoalClient[IO] = new SavingsGoalClient[IO] {
      def getGoal(
          goalId: UUID,
          accountUid: UUID
      ): IO[Either[AppError, SavingsGoal]] =
        IO.pure(getGoalResult)
      def createGoal(accountUid: UUID): IO[Either[AppError, UUID]] =
        IO.pure(createGoalResult)

      override def transferToGoal(
          accountUid: UUID,
          goal: UUID,
          amountMinorUnits: Long
      ): IO[Either[AppError, AddMoneyResponse]] =
        IO.pure(Left(GenericError("transferToGoal not implemented in test")))
    }

    GoalService.impl[IO]
  }

  test("getOrCreateGoal creates and persists goal when no existing goal") {
    val service = mkService(
      readGoalResult = Right(None),
      persistGoalResult = Right(()),
      createGoalResult = Right(testGoalId)
    )
    service.getOrCreateGoal(testConfig, testAccountId).map { result =>
      assertEquals(result, Right(testGoalId))
    }
  }

  test("getOrCreateGoal reads and validates existing goal") {
    val existingId = UUID.randomUUID()
    val service = mkService(
      readGoalResult = Right(Some(existingId)),
      persistGoalResult = Right(()),
      getGoalResult = Right(createTestSavingsGoal(existingId))
    )
    service.getOrCreateGoal(testConfig, testAccountId).map { result =>
      assertEquals(result, Right(existingId))
    }
  }

  test("returns error when readGoal fails") {
    val err = FileReadError("read failed")
    val service = mkService(readGoalResult = Left(err))
    service.getOrCreateGoal(testConfig, testAccountId).map { result =>
      assertEquals(result, Left(err))
    }
  }

  test("returns error when getGoal API fails") {
    val existingId = UUID.randomUUID()
    val apiErr = NotFoundError("not found")
    val service = mkService(
      readGoalResult = Right(Some(existingId)),
      getGoalResult = Left(apiErr)
    )
    service.getOrCreateGoal(testConfig, testAccountId).map { result =>
      assertEquals(result, Left(apiErr))
    }
  }

  test("returns error when createGoal fails") {
    val err = GenericError("create failed")
    val service = mkService(
      readGoalResult = Right(None),
      createGoalResult = Left(err)
    )
    service.getOrCreateGoal(testConfig, testAccountId).map { result =>
      assertEquals(result, Left(err))
    }
  }

  test("returns error when persistGoal fails after create") {
    val err = FileWriteError("persist failed")
    val service = mkService(
      readGoalResult = Right(None),
      createGoalResult = Right(testGoalId),
      persistGoalResult = Left(err)
    )
    service.getOrCreateGoal(testConfig, testAccountId).map { result =>
      assertEquals(result, Left(err))
    }
  }

  test("returns error when persistGoal fails after validation") {
    val existingId = UUID.randomUUID()
    val err = FileWriteError("persist after validation failed")
    val service = mkService(
      readGoalResult = Right(Some(existingId)),
      getGoalResult = Right(createTestSavingsGoal(existingId)),
      persistGoalResult = Left(err)
    )
    service.getOrCreateGoal(testConfig, testAccountId).map { result =>
      assertEquals(result, Left(err))
    }
  }
}
