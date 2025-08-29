package com.adewusi.roundup.starlingapis

import cats.effect.IO
import com.adewusi.roundup.delete.RoundupRoutes
import com.adewusi.roundup.model._
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.implicits._

import java.util.UUID

class StarlingSavingsGoalApiCreateSpec extends CatsEffectSuite {

  private val testAccountUid = "test-account-uid"
  private val testSavingsGoalUid = UUID.randomUUID()

  private val testCreateRequest = CreateSavingsGoalRequest(
    name = "Trip to Paris",
    currency = "GBP",
    target = Some(CurrencyAndAmount("GBP", 100000)),
    base64EncodedPhoto = None
  )

  private val testCreateResponse = CreateSavingsGoalResponse(
    savingsGoalUid = testSavingsGoalUid,
    success = true
  )

  test("Create SavingsGoal route returns status code 200") {
    assertIO(retCreateSavingsGoal.map(_.status), Status.Ok)
  }

  test("Create SavingsGoal route returns correct response") {
    assertIO(
      retCreateSavingsGoal.flatMap(_.as[CreateSavingsGoalResponse]).map(_.success),
      true
    )
  }

  test("Create SavingsGoal route returns correct savings goal UID") {
    assertIO(
      retCreateSavingsGoal.flatMap(_.as[CreateSavingsGoalResponse]).map(_.savingsGoalUid),
      testSavingsGoalUid
    )
  }

  test("Create SavingsGoal route with minimal request returns status code 200") {
    assertIO(retCreateSavingsGoalMinimal.map(_.status), Status.Ok)
  }

  test("Create SavingsGoal route with photo returns status code 200") {
    assertIO(retCreateSavingsGoalWithPhoto.map(_.status), Status.Ok)
  }

  test("Create SavingsGoal route returns 404 for invalid path") {
    assertIO(retInvalidPath.map(_.status), Status.NotFound)
  }

  test("Create SavingsGoal route returns 400 for invalid JSON") {
    assertIO(retInvalidJson.map(_.status), Status.BadRequest)
  }

  private[this] val retCreateSavingsGoal: IO[Response[IO]] = {
    val request = Request[IO](Method.PUT, uri"/account" / testAccountUid / "savings-goals")
      .withEntity(testCreateRequest)
    val mockSavingsGoals = createMockSavingsGoals(testCreateResponse)
    RoundupRoutes.savingsGoalsRoutes(mockSavingsGoals).orNotFound(request)
  }

  private[this] val retCreateSavingsGoalMinimal: IO[Response[IO]] = {
    val minimalRequest = CreateSavingsGoalRequest(
      name = "Emergency Fund",
      currency = "GBP"
    )
    val request = Request[IO](Method.PUT, uri"/account" / testAccountUid / "savings-goals")
      .withEntity(minimalRequest)
    val mockSavingsGoals = createMockSavingsGoals(testCreateResponse)
    RoundupRoutes.savingsGoalsRoutes(mockSavingsGoals).orNotFound(request)
  }

  private[this] val retCreateSavingsGoalWithPhoto: IO[Response[IO]] = {
    val requestWithPhoto = testCreateRequest.copy(
      base64EncodedPhoto = Some("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==")
    )
    val request = Request[IO](Method.PUT, uri"/account" / testAccountUid / "savings-goals")
      .withEntity(requestWithPhoto)
    val mockSavingsGoals = createMockSavingsGoals(testCreateResponse)
    RoundupRoutes.savingsGoalsRoutes(mockSavingsGoals).orNotFound(request)
  }

  private[this] val retInvalidPath: IO[Response[IO]] = {
    val request = Request[IO](Method.PUT, uri"/invalid-path")
      .withEntity(testCreateRequest)
    val mockSavingsGoals = createMockSavingsGoals(testCreateResponse)
    RoundupRoutes.savingsGoalsRoutes(mockSavingsGoals).orNotFound(request)
  }

  private[this] val retInvalidJson: IO[Response[IO]] = {
    val request = Request[IO](Method.PUT, uri"/account" / testAccountUid / "savings-goals")
      .withEntity("{invalid json}")
    val mockSavingsGoals = createMockSavingsGoals(testCreateResponse)
    RoundupRoutes.savingsGoalsRoutes(mockSavingsGoals).orNotFound(request)
  }

  private def createMockSavingsGoals(response: CreateSavingsGoalResponse): StarlingSavingsGoalsApi[IO] = {
    new StarlingSavingsGoalsApi[IO] {
      override def getSavingsGoals(accountUid: String): IO[SavingsGoalsResponse] = {
        IO.pure(SavingsGoalsResponse(List.empty))
      }

      override def createSavingsGoal(accountUid: String, request: CreateSavingsGoalRequest): IO[CreateSavingsGoalResponse] = {
        IO.pure(response)
      }

      override def addMoney(accountUid: String, savingsGoalUid: String, transferUid: String, request: AddMoneyRequest): IO[AddMoneyResponse] = ???
    }
  }
}