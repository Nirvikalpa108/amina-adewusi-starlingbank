package com.adewusi.roundup.starlingapis

import cats.effect.IO
import com.adewusi.roundup.RoundupRoutes
import com.adewusi.roundup.model._
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.headers.`Content-Type`
import org.http4s.implicits._

import java.util.UUID

class StarlingSavingsGoalsApiAddMoneySpec extends CatsEffectSuite {

  private val accountUid = "acc-123"
  private val savingsGoalUid = "sg-456"
  private val transferUid = "tx-789"

  test("addMoney returns status code 200 for valid request") {
    assertIO(
      retAddMoney(validAddMoneyRequest).map(_.status),
      Status.Ok
    )
  }

  test("addMoney returns correct response body for valid request") {
    val expectedResponse = AddMoneyResponse(
      transferUid = UUID.fromString("acf4b824-205d-4a6f-b363-635a4550eae2"),
      success = true
    )

    assertIO(
      retAddMoney(validAddMoneyRequest).flatMap(_.as[AddMoneyResponse]),
      expectedResponse
    )
  }

  test("addMoney returns status code 400 for malformed JSON") {
    val malformedJson =
      """{"amount": {"currency": "GBP", "minorUnits": "not-a-number"}}"""
    val request = Request[IO](
      Method.PUT,
      uri"/account" / accountUid / "savings-goals" / savingsGoalUid / "add-money" / transferUid
    ).withEntity(malformedJson)
      .withContentType(`Content-Type`(MediaType.application.json))

    assertIO(
      fakeSavingsGoalsRoutes.orNotFound(request).map(_.status),
      Status.BadRequest
    )
  }

  test("addMoney returns status code 400 for invalid JSON") {
    val invalidJson = """invalid json"""
    val request = Request[IO](
      Method.PUT,
      uri"/account" / accountUid / "savings-goals" / savingsGoalUid / "add-money" / transferUid
    ).withEntity(invalidJson)

    assertIO(
      fakeSavingsGoalsRoutes.orNotFound(request).map(_.status),
      Status.BadRequest
    )
  }

  test("addMoney returns status code 500 for service error") {
    val errorService = new StarlingSavingsGoalsApi[IO] {
      override def getSavingsGoals(
          accountUid: String
      ): IO[SavingsGoalsResponse] =
        IO.raiseError(new RuntimeException("not used"))

      override def createSavingsGoal(
          accountUid: String,
          request: CreateSavingsGoalRequest
      ): IO[CreateSavingsGoalResponse] =
        IO.raiseError(new RuntimeException("not used"))

      override def addMoney(
          accountUid: String,
          savingsGoalUid: String,
          transferUid: String,
          request: AddMoneyRequest
      ): IO[AddMoneyResponse] =
        IO.raiseError(new RuntimeException("Service error"))
    }

    val request = Request[IO](
      Method.PUT,
      uri"/account" / accountUid / "savings-goals" / savingsGoalUid / "add-money" / transferUid
    ).withEntity(validAddMoneyRequest)

    assertIO(
      RoundupRoutes
        .savingsGoalsRoutes(errorService)
        .orNotFound(request)
        .map(_.status),
      Status.InternalServerError
    )
  }

  private val validAddMoneyRequest = AddMoneyRequest(
    amount = CurrencyAndAmount(currency = "GBP", minorUnits = 123456),
    reference = Some("test reference")
  )

  private def retAddMoney(request: AddMoneyRequest): IO[Response[IO]] = {
    val putRequest = Request[IO](
      Method.PUT,
      uri"/account" / accountUid / "savings-goals" / savingsGoalUid / "add-money" / transferUid
    ).withEntity(request)

    fakeSavingsGoalsRoutes.orNotFound(putRequest)
  }

  private val fakeSavingsGoalsRoutes = {
    val fakeSavingsGoals = new StarlingSavingsGoalsApi[IO] {
      override def getSavingsGoals(
          accountUid: String
      ): IO[SavingsGoalsResponse] = ???

      override def createSavingsGoal(
          accountUid: String,
          request: CreateSavingsGoalRequest
      ): IO[CreateSavingsGoalResponse] = ???

      override def addMoney(
          accountUid: String,
          savingsGoalUid: String,
          transferUid: String,
          request: AddMoneyRequest
      ): IO[AddMoneyResponse] =
        IO.pure(
          AddMoneyResponse(
            transferUid =
              UUID.fromString("acf4b824-205d-4a6f-b363-635a4550eae2"),
            success = true
          )
        )
    }

    RoundupRoutes.savingsGoalsRoutes(fakeSavingsGoals)
  }
}
