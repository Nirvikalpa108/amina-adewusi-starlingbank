package com.adewusi.roundup.clients

import cats.effect.IO
import com.adewusi.roundup.model._
import com.adewusi.roundup.starlingapis.StarlingSavingsGoalsApi
import munit.CatsEffectSuite

import java.util.UUID

class SavingsGoalClientSpec extends CatsEffectSuite {

  private val accountUid = UUID.randomUUID()
  private val goalUid = UUID.randomUUID()

  private val sampleGoal = SavingsGoal(
    savingsGoalUid = goalUid,
    name = "Holiday",
    target = None,
    totalSaved = CurrencyAndAmount("GBP", 1000),
    savedPercentage = Some(10),
    state = "ACTIVE"
  )

  private def makeApi(
      getGoalsResult: Either[Throwable, SavingsGoalsResponse] = Right(
        SavingsGoalsResponse(Nil)
      ),
      createGoalResult: Either[Throwable, CreateSavingsGoalResponse] = Left(
        new NotImplementedError("unused")
      ),
      addMoneyResult: Either[Throwable, AddMoneyResponse] = Left(
        new NotImplementedError("unused")
      )
  ): StarlingSavingsGoalsApi[IO] =
    new StarlingSavingsGoalsApi[IO] {
      override def getSavingsGoals(
          accountUid: String
      ): IO[SavingsGoalsResponse] =
        IO.fromEither(getGoalsResult)

      override def createSavingsGoal(
          accountUid: String,
          request: CreateSavingsGoalRequest
      ): IO[CreateSavingsGoalResponse] =
        IO.fromEither(createGoalResult)

      override def addMoney(
          accountUid: String,
          savingsGoalUid: String,
          transferUid: String,
          request: AddMoneyRequest
      ): IO[AddMoneyResponse] =
        IO.fromEither(addMoneyResult)
    }

  // ----------------------
  // getGoal tests
  // ----------------------
  test("getGoal should return Right(goal) if goal exists") {
    implicit val api: StarlingSavingsGoalsApi[IO] =
      makeApi(getGoalsResult = Right(SavingsGoalsResponse(List(sampleGoal))))
    val client = SavingsGoalClient.impl[IO]

    client.getGoal(goalUid, accountUid).assertEquals(Right(sampleGoal))
  }

  test(
    "getGoal should return Left(InvalidStoredSavingsGoalUuid) if goal not found"
  ) {
    implicit val api: StarlingSavingsGoalsApi[IO] = makeApi(getGoalsResult = Right(SavingsGoalsResponse(Nil)))
    val client = SavingsGoalClient.impl[IO]

    client.getGoal(goalUid, accountUid).map {
      case Left(InvalidStoredSavingsGoalUuid(msg)) =>
        assert(msg.contains(goalUid.toString))
      case other =>
        fail(s"Expected InvalidStoredSavingsGoalUuid, got $other")
    }
  }

  test("getGoal should return Left(GenericError) if API call fails") {
    implicit val api: StarlingSavingsGoalsApi[IO] = makeApi(getGoalsResult = Left(new RuntimeException("boom")))
    val client = SavingsGoalClient.impl[IO]

    client.getGoal(goalUid, accountUid).map {
      case Left(GenericError(msg)) =>
        assert(msg.contains("boom"))
      case other =>
        fail(s"Expected GenericError, got $other")
    }
  }

  // ----------------------
  // createGoal tests
  // ----------------------
  test("createGoal should return Right(savingsGoalUid) if creation succeeds") {
    val expectedUid = UUID.randomUUID()
    implicit val api: StarlingSavingsGoalsApi[IO] = makeApi(
      createGoalResult = Right(
        CreateSavingsGoalResponse(savingsGoalUid = expectedUid, success = true)
      )
    )
    val client = SavingsGoalClient.impl[IO]

    client.createGoal(accountUid).assertEquals(Right(expectedUid))
  }

  test(
    "createGoal should return Left(GenericError) if API returns success=false"
  ) {
    implicit val api: StarlingSavingsGoalsApi[IO] = makeApi(
      createGoalResult = Right(
        CreateSavingsGoalResponse(
          savingsGoalUid = UUID.randomUUID(),
          success = false
        )
      )
    )
    val client = SavingsGoalClient.impl[IO]

    client.createGoal(accountUid).map {
      case Left(GenericError(msg)) =>
        assert(msg.contains("API returned false"))
      case other =>
        fail(s"Expected GenericError, got $other")
    }
  }

  test(
    "createGoal should return Left(GenericError) if API call fails with exception"
  ) {
    implicit val api: StarlingSavingsGoalsApi[IO] = makeApi(
      createGoalResult = Left(new RuntimeException("kaboom"))
    )
    val client = SavingsGoalClient.impl[IO]

    client.createGoal(accountUid).map {
      case Left(GenericError(msg)) =>
        assert(msg.contains("kaboom"))
      case other =>
        fail(s"Expected GenericError, got $other")
    }
  }

  // ----------------------
  // transferToGoal tests
  // ----------------------
  test(
    "transferToGoal should return Right(AddMoneyResponse) if transfer succeeds"
  ) {
    val expectedTransferUid = UUID.randomUUID()
    val expectedResponse =
      AddMoneyResponse(transferUid = expectedTransferUid, success = true)
    implicit val api: StarlingSavingsGoalsApi[IO] = makeApi(
      addMoneyResult = Right(expectedResponse)
    )
    val client = SavingsGoalClient.impl[IO]

    client
      .transferToGoal(accountUid, goalUid, 158L)
      .assertEquals(Right(expectedResponse))
  }

  test(
    "transferToGoal should return Left(TransferError) if API returns success=false"
  ) {
    implicit val api: StarlingSavingsGoalsApi[IO] = makeApi(
      addMoneyResult = Right(
        AddMoneyResponse(transferUid = UUID.randomUUID(), success = false)
      )
    )
    val client = SavingsGoalClient.impl[IO]

    client.transferToGoal(accountUid, goalUid, 158L).map {
      case Left(TransferError(msg)) =>
        assert(msg.contains("API returned success=false"))
      case other =>
        fail(s"Expected TransferError, got $other")
    }
  }

  test(
    "transferToGoal should return Left(TransferError) if API call fails with exception"
  ) {
    implicit val api: StarlingSavingsGoalsApi[IO] = makeApi(
      addMoneyResult = Left(new RuntimeException("network error"))
    )
    val client = SavingsGoalClient.impl[IO]

    client.transferToGoal(accountUid, goalUid, 158L).map {
      case Left(TransferError(msg)) =>
        assert(msg.contains("network error"))
      case other =>
        fail(s"Expected TransferError, got $other")
    }
  }
}
