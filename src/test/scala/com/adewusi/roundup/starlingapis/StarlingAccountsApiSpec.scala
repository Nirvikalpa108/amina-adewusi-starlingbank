package com.adewusi.roundup.starlingapis

import cats.effect.IO
import cats.implicits.catsSyntaxApplicativeId
import com.adewusi.roundup.TestUtils
import com.adewusi.roundup.model.{Account, AccountsResponse}
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.headers.{Authorization, `Content-Type`}
import org.typelevel.ci.CIStringSyntax

import java.util.UUID

class StarlingAccountsApiSpec extends CatsEffectSuite with TestUtils {

  private def withApi(httpApp: HttpApp[IO])(f: StarlingAccountsApi[IO] => IO[Unit]): IO[Unit] = {
    val client = Client.fromHttpApp(httpApp)
    val api    = StarlingAccountsApi.impl[IO](client)
    f(api)
  }

  private def assertRequest(request: Request[IO]): Unit = {
    assertEquals(request.method, Method.GET)
    assertEquals(
      request.uri.renderString,
      s"${testConfig.starling.baseUri}/api/v2/accounts"
    )

    assertEquals(
      request.headers.get[Authorization].map(_.credentials.toString),
      Some(s"Bearer $testToken")
    )

    // Fix: get raw headers by name
    assertEquals(
      request.headers.get(ci"Accept").map(_.head.value),
      Some("application/json")
    )
    assertEquals(
      request.headers.get(ci"User-Agent").map(_.head.value),
      Some("Adewusi")
    )
  }

  test("getAccounts should return accounts when API call succeeds") {
    val mockAccount = Account(
      accountUid      = UUID.randomUUID(),
      accountType     = "PRIMARY",
      defaultCategory = UUID.randomUUID(),
      currency        = "GBP",
      createdAt       = "2023-01-01T00:00:00.000Z",
      name            = "Personal"
    )
    val expectedResponse = AccountsResponse(List(mockAccount))

    val httpApp = HttpApp[IO] { req =>
      IO(assertRequest(req)) *>
        Response[IO](Status.Ok).withEntity(expectedResponse).pure[IO]
    }

    withApi(httpApp) { api =>
      api.getAccounts(testConfig.starling.accessToken, testConfig.starling.baseUri).map { response =>
        assertEquals(response, expectedResponse)
        assertEquals(response.accounts.head.currency, "GBP")
      }
    }
  }

  test("getAccounts should handle API errors") {
    val httpApp = HttpApp[IO](_ => Response[IO](Status.Unauthorized).pure[IO])

    withApi(httpApp) { api =>
      api.getAccounts(testConfig.starling.accessToken, testConfig.starling.baseUri).attempt.map { result =>
        assert(result.isLeft, "Should fail with HTTP error")
      }
    }
  }

  test("getAccounts should handle malformed JSON response") {
    val httpApp = HttpApp[IO] { _ =>
      Response[IO](Status.Ok)
        .withEntity("invalid json")
        .withContentType(`Content-Type`(MediaType.application.json))
        .pure[IO]
    }

    withApi(httpApp) { api =>
      api.getAccounts(testConfig.starling.accessToken, testConfig.starling.baseUri).attempt.map { result =>
        assert(result.isLeft, "Should fail with JSON parsing error")
      }
    }
  }
}