package com.adewusi.roundup.starlingapis

import cats.effect.IO
import cats.implicits.catsSyntaxApplicativeId
import com.adewusi.roundup.model._
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.headers.{Authorization, `Content-Type`}
import org.typelevel.ci.CIStringSyntax

import java.util.UUID

class StarlingSavingsGoalsApiSpec extends CatsEffectSuite {

  private val testToken          = "test-token"
  private val testAccountUid     = "test-account-uid"
  private val testSavingsGoalUid = UUID.randomUUID()
  private val testTransferUid    = UUID.randomUUID()

  private val testCreateRequest = CreateSavingsGoalRequest(
    name = "Trip to Paris",
    currency = "GBP",
    target = Some(CurrencyAndAmount("GBP", 100000)), // £1000.00
    base64EncodedPhoto = None
  )

  private val testCreateResponse = CreateSavingsGoalResponse(
    savingsGoalUid = testSavingsGoalUid,
    success = true
  )

  private val mockConfig = AppConfig(
    starling = StarlingConfig(
      accessToken   = testToken,
      baseUrl       = "https://api-sandbox.starlingbank.com",
      initialGoalId = None
    )
  )

  private def withApi(httpApp: HttpApp[IO])(f: StarlingSavingsGoalsApi[IO] => IO[Unit]): IO[Unit] = {
    val client = Client.fromHttpApp(httpApp)
    val api    = StarlingSavingsGoalsApi.impl[IO](client, mockConfig)
    f(api)
  }

  private def assertCommonHeaders(request: Request[IO]): Unit = {
    assertEquals(
      request.headers.get[Authorization].map(_.credentials.toString),
      Some(s"Bearer $testToken")
    )
    assertEquals(
      request.headers.get(ci"Accept").map(_.head.value),
      Some("application/json")
    )
    assertEquals(
      request.headers.get(ci"User-Agent").map(_.head.value),
      Some("Adewusi")
    )
  }

  test("getSavingsGoals should return savings goals when API call succeeds") {
    val mockSavingsGoal = SavingsGoal(
      savingsGoalUid   = testSavingsGoalUid,
      name             = "Holiday Fund",
      target           = Some(CurrencyAndAmount("GBP", 100000)),
      totalSaved       = CurrencyAndAmount("GBP", 25000),
      savedPercentage  = Some(25),
      state            = "ACTIVE"
    )
    val expectedResponse = SavingsGoalsResponse(List(mockSavingsGoal))

    val httpApp = HttpApp[IO] { req =>
      IO {
        assertEquals(req.method, Method.GET)
        assertEquals(
          req.uri.renderString,
          s"${mockConfig.starling.baseUrl}/api/v2/account/$testAccountUid/savings-goals"
        )
        assertCommonHeaders(req)
      } *> Response[IO](Status.Ok).withEntity(expectedResponse).pure[IO]
    }

    withApi(httpApp) { api =>
      api.getSavingsGoals(testAccountUid).map { response =>
        assertEquals(response.savingsGoalList.head.name, "Holiday Fund")
        assertEquals(response.savingsGoalList.head.state, "ACTIVE")
      }
    }
  }

  test("createSavingsGoal should create savings goal when API call succeeds") {
    val httpApp = HttpApp[IO] { req =>
      IO {
        assertEquals(req.method, Method.PUT)
        assertEquals(
          req.uri.renderString,
          s"${mockConfig.starling.baseUrl}/api/v2/account/$testAccountUid/savings-goals"
        )
        assertCommonHeaders(req)
        assertEquals(
          req.headers.get(ci"Content-Type").map(_.head.value),
          Some("application/json")
        )
      } *> Response[IO](Status.Ok).withEntity(testCreateResponse).pure[IO]
    }

    withApi(httpApp) { api =>
      api.createSavingsGoal(testAccountUid, testCreateRequest).map { response =>
        assertEquals(response, testCreateResponse)
        assert(response.success)
      }
    }
  }

  test("addMoney should transfer money to savings goal when API call succeeds") {
    val addMoneyRequest = AddMoneyRequest(CurrencyAndAmount("GBP", 1000))
    val expectedResponse = AddMoneyResponse(
      transferUid = testTransferUid,
      success     = true
    )

    val httpApp = HttpApp[IO] { req =>
      IO {
        assertEquals(req.method, Method.PUT)
        assertEquals(
          req.uri.renderString,
          s"${mockConfig.starling.baseUrl}/api/v2/account/$testAccountUid/savings-goals/$testSavingsGoalUid/add-money/$testTransferUid"
        )
        assertCommonHeaders(req)
        assertEquals(
          req.headers.get(ci"Content-Type").map(_.head.value),
          Some("application/json")
        )
      } *> Response[IO](Status.Ok).withEntity(expectedResponse).pure[IO]
    }

    withApi(httpApp) { api =>
      api.addMoney(testAccountUid, testSavingsGoalUid.toString, testTransferUid.toString, addMoneyRequest).map { response =>
        assertEquals(response, expectedResponse)
        assert(response.success)
      }
    }
  }

  test("getSavingsGoals should handle API errors") {
    val httpApp = HttpApp[IO](_ => Response[IO](Status.NotFound).pure[IO])

    withApi(httpApp) { api =>
      api.getSavingsGoals(testAccountUid).attempt.map { result =>
        assert(result.isLeft, "Should fail with HTTP error")
      }
    }
  }

  test("createSavingsGoal should handle malformed JSON response") {
    val httpApp = HttpApp[IO] { _ =>
      Response[IO](Status.Ok)
        .withEntity("invalid json")
        .withContentType(`Content-Type`(MediaType.application.json))
        .pure[IO]
    }

    withApi(httpApp) { api =>
      api.createSavingsGoal(testAccountUid, testCreateRequest).attempt.map { result =>
        assert(result.isLeft, "Should fail with JSON parsing error")
      }
    }
  }

  test("addMoney should handle API errors") {
    val addMoneyRequest = AddMoneyRequest(CurrencyAndAmount("GBP", 1000))
    val httpApp = HttpApp[IO](_ => Response[IO](Status.BadRequest).pure[IO])

    withApi(httpApp) { api =>
      api.addMoney(testAccountUid, testSavingsGoalUid.toString, testTransferUid.toString, addMoneyRequest).attempt.map { result =>
        assert(result.isLeft, "Should fail with HTTP error")
      }
    }
  }
}