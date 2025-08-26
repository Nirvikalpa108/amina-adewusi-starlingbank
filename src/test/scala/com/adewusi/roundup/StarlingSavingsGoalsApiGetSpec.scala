package com.adewusi.roundup

import cats.effect.IO
import com.adewusi.roundup.model.{AddMoneyRequest, AddMoneyResponse, CreateSavingsGoalRequest, CreateSavingsGoalResponse, CurrencyAndAmount, SavingsGoal, SavingsGoalsResponse}
import com.adewusi.roundup.starlingapis.StarlingSavingsGoalsApi
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.implicits._

import java.util.UUID

class StarlingSavingsGoalsApiGetSpec extends CatsEffectSuite {

  private val testAccountUid = "test-account-uid"
  private val testSavingsGoalUid = UUID.randomUUID()

  private val testSavingsGoal = SavingsGoal(
    savingsGoalUid = testSavingsGoalUid,
    name = "Holiday Fund",
    target = Some(CurrencyAndAmount("GBP", 100000)),
    totalSaved = CurrencyAndAmount("GBP", 25000),
    savedPercentage = Some(25),
    state = "ACTIVE"
  )

  private val testSavingsGoalsResponse = SavingsGoalsResponse(
    savingsGoalList = List(testSavingsGoal)
  )

  test("SavingsGoals route returns status code 200") {
    assertIO(retSavingsGoals.map(_.status), Status.Ok)
  }

  test("SavingsGoals route returns correct savings goals response") {
    assertIO(
      retSavingsGoals.flatMap(_.as[SavingsGoalsResponse]).map(_.savingsGoalList.length),
      1
    )
  }

  test("SavingsGoals route returns correct savings goal name") {
    assertIO(
      retSavingsGoals.flatMap(_.as[SavingsGoalsResponse]).map(_.savingsGoalList.head.name),
      "Holiday Fund"
    )
  }

  test("SavingsGoals route returns correct savings goal state") {
    assertIO(
      retSavingsGoals.flatMap(_.as[SavingsGoalsResponse]).map(_.savingsGoalList.head.state),
      "ACTIVE"
    )
  }

  test("SavingsGoals route with empty list returns status code 200") {
    assertIO(retEmptySavingsGoals.map(_.status), Status.Ok)
  }

  test("SavingsGoals route with empty list returns empty array") {
    assertIO(
      retEmptySavingsGoals.flatMap(_.as[SavingsGoalsResponse]).map(_.savingsGoalList.length),
      0
    )
  }

  test("SavingsGoals route with multiple goals returns correct count") {
    assertIO(
      retMultipleSavingsGoals.flatMap(_.as[SavingsGoalsResponse]).map(_.savingsGoalList.length),
      2
    )
  }

  test("SavingsGoals route returns 404 for invalid path") {
    assertIO(retInvalidPath.map(_.status), Status.NotFound)
  }

  private[this] val retSavingsGoals: IO[Response[IO]] = {
    val request = Request[IO](Method.GET, uri"/savings-goals" / testAccountUid)
    val mockSavingsGoals = createMockSavingsGoals(testSavingsGoalsResponse)
    RoundupRoutes.savingsGoalsRoutes(mockSavingsGoals).orNotFound(request)
  }

  private[this] val retEmptySavingsGoals: IO[Response[IO]] = {
    val request = Request[IO](Method.GET, uri"/savings-goals" / testAccountUid)
    val emptySavingsGoalsResponse = SavingsGoalsResponse(savingsGoalList = List.empty)
    val mockSavingsGoals = createMockSavingsGoals(emptySavingsGoalsResponse)
    RoundupRoutes.savingsGoalsRoutes(mockSavingsGoals).orNotFound(request)
  }

  private[this] val retMultipleSavingsGoals: IO[Response[IO]] = {
    val secondSavingsGoal = SavingsGoal(
      savingsGoalUid = UUID.randomUUID(),
      name = "Emergency Fund",
      target = Some(CurrencyAndAmount("GBP", 500000)),
      totalSaved = CurrencyAndAmount("GBP", 150000),
      savedPercentage = Some(30),
      state = "ACTIVE"
    )
    val multipleSavingsGoalsResponse = SavingsGoalsResponse(
      savingsGoalList = List(testSavingsGoal, secondSavingsGoal)
    )
    val request = Request[IO](Method.GET, uri"/savings-goals" / testAccountUid)
    val mockSavingsGoals = createMockSavingsGoals(multipleSavingsGoalsResponse)
    RoundupRoutes.savingsGoalsRoutes(mockSavingsGoals).orNotFound(request)
  }

  private[this] val retInvalidPath: IO[Response[IO]] = {
    val request = Request[IO](Method.GET, uri"/invalid-path")
    val mockSavingsGoals = createMockSavingsGoals(testSavingsGoalsResponse)
    RoundupRoutes.savingsGoalsRoutes(mockSavingsGoals).orNotFound(request)
  }

  private def createMockSavingsGoals(response: SavingsGoalsResponse): StarlingSavingsGoalsApi[IO] = {
    new StarlingSavingsGoalsApi[IO] {
      override def getSavingsGoals(accountUid: String): IO[SavingsGoalsResponse] = {
        IO.pure(response)
      }

      override def createSavingsGoal(accountUid: String, request: CreateSavingsGoalRequest): IO[CreateSavingsGoalResponse] = ???

      override def addMoney(accountUid: String, savingsGoalUid: String, transferUid: String, request: AddMoneyRequest): IO[AddMoneyResponse] = ???
    }
  }
}