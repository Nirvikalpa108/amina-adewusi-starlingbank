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
      getGoalsResult: Either[Throwable, SavingsGoalsResponse],
      createGoalResult: Either[Throwable, CreateSavingsGoalResponse],
      addMoneyResult: Either[Throwable, AddMoneyResponse]
  ): StarlingSavingsGoalsApi[IO] =
    new StarlingSavingsGoalsApi[IO] {
      override def getSavingsGoals(
          accountUid: String
      ): IO[SavingsGoalsResponse] = IO.fromEither(getGoalsResult)
      override def createSavingsGoal(
          accountUid: String,
          request: CreateSavingsGoalRequest
      ): IO[CreateSavingsGoalResponse] = IO.fromEither(createGoalResult)
      override def addMoney(
          accountUid: String,
          savingsGoalUid: String,
          transferUid: String,
          request: AddMoneyRequest
      ): IO[AddMoneyResponse] = IO.fromEither(addMoneyResult)
    }

  private def withClient(
      getGoalsResult: Either[Throwable, SavingsGoalsResponse] = Right(
        SavingsGoalsResponse(Nil)
      ),
      createGoalResult: Either[Throwable, CreateSavingsGoalResponse] = Left(
        new NotImplementedError("unused")
      ),
      addMoneyResult: Either[Throwable, AddMoneyResponse] = Left(
        new NotImplementedError("unused")
      )
  )(testBody: SavingsGoalClient[IO] => IO[Unit]): IO[Unit] = {
    implicit val api: StarlingSavingsGoalsApi[IO] =
      makeApi(getGoalsResult, createGoalResult, addMoneyResult)
    val client = SavingsGoalClient.impl[IO]
    testBody(client)
  }

  // ----------------------
  // getGoal tests
  // ----------------------

  test("getGoal should return Right(goal) if goal exists") {
    withClient(getGoalsResult = Right(SavingsGoalsResponse(List(sampleGoal)))) {
      client =>
        client.getGoal(goalUid, accountUid).assertEquals(Right(sampleGoal))
    }
  }

  test(
    "getGoal should return Left(InvalidStoredSavingsGoalUuid) if goal not found"
  ) {
    withClient(getGoalsResult = Right(SavingsGoalsResponse(Nil))) { client =>
      client.getGoal(goalUid, accountUid).map {
        case Left(InvalidStoredSavingsGoalUuid(msg)) =>
          assert(msg.contains(goalUid.toString))
        case other => fail(s"Expected InvalidStoredSavingsGoalUuid, got $other")
      }
    }
  }

  test("getGoal should return Left(GenericError) if API call fails") {
    withClient(getGoalsResult = Left(new RuntimeException("error"))) { client =>
      client.getGoal(goalUid, accountUid).map {
        case Left(GenericError(msg)) => assert(msg.contains("error"))
        case other => fail(s"Expected GenericError, got $other")
      }
    }
  }

  // ----------------------
  // createGoal tests
  // ----------------------

  test("createGoal should return Right(savingsGoalUid) if creation succeeds") {
    val expectedUid = UUID.randomUUID()
    withClient(createGoalResult =
      Right(
        CreateSavingsGoalResponse(savingsGoalUid = expectedUid, success = true)
      )
    ) { client =>
      client.createGoal(accountUid).assertEquals(Right(expectedUid))
    }
  }

  test(
    "createGoal should return Left(GenericError) if API returns success=false"
  ) {
    withClient(createGoalResult =
      Right(
        CreateSavingsGoalResponse(
          savingsGoalUid = UUID.randomUUID(),
          success = false
        )
      )
    ) { client =>
      client.createGoal(accountUid).map {
        case Left(GenericError(msg)) =>
          assert(msg.contains("API returned false"))
        case other => fail(s"Expected GenericError, got $other")
      }
    }
  }

  test(
    "createGoal should return Left(GenericError) if API call fails with exception"
  ) {
    withClient(createGoalResult = Left(new RuntimeException("kaboom"))) {
      client =>
        client.createGoal(accountUid).map {
          case Left(GenericError(msg)) => assert(msg.contains("kaboom"))
          case other => fail(s"Expected GenericError, got $other")
        }
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
    withClient(addMoneyResult = Right(expectedResponse)) { client =>
      client
        .transferToGoal(
          accountUid = accountUid,
          goal = goalUid,
          amountMinorUnits = 158L
        )
        .assertEquals(Right(expectedResponse))
    }
  }

  test(
    "transferToGoal should return Left(TransferError) if API returns success=false"
  ) {
    withClient(addMoneyResult =
      Right(AddMoneyResponse(transferUid = UUID.randomUUID(), success = false))
    ) { client =>
      client
        .transferToGoal(
          accountUid = accountUid,
          goal = goalUid,
          amountMinorUnits = 158L
        )
        .map {
          case Left(TransferError(msg)) =>
            assert(msg.contains("API returned success=false"))
          case other => fail(s"Expected TransferError, got $other")
        }
    }
  }

  test(
    "transferToGoal should return Left(TransferError) if API call fails with exception"
  ) {
    withClient(addMoneyResult = Left(new RuntimeException("network error"))) {
      client =>
        client
          .transferToGoal(
            accountUid = accountUid,
            goal = goalUid,
            amountMinorUnits = 158L
          )
          .map {
            case Left(TransferError(msg)) =>
              assert(msg.contains("network error"))
            case other => fail(s"Expected TransferError, got $other")
          }
    }
  }
}
