package com.adewusi.roundup.starlingapis

import cats.effect.IO
import cats.implicits.catsSyntaxApplicativeId
import com.adewusi.roundup.TestUtils
import com.adewusi.roundup.model._
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.headers.{Authorization, `Content-Type`}
import org.typelevel.ci.CIStringSyntax

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class StarlingTransactionApiSpec extends CatsEffectSuite with TestUtils {

  private val testAccountUid  = UUID.randomUUID()

  private val sampleTxFeedResp: TransactionFeedResponse =
    TransactionFeedResponse(feedItems = List.empty)

  private val minTs: ZonedDateTime = ZonedDateTime.parse("2023-08-01T00:00:00Z")
  private val maxTs: ZonedDateTime = ZonedDateTime.parse("2023-08-08T23:59:59Z")
  private val fmt                  = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  private def withApi(httpApp: HttpApp[IO])(f: StarlingTransactionApi[IO] => IO[Unit]): IO[Unit] = {
    val client = Client.fromHttpApp(httpApp)
    val api    = StarlingTransactionApi.impl[IO](client, testConfig)
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

  test("getSettledTransactionsBetween should return transactions when API call succeeds") {
    val httpApp = HttpApp[IO] { req =>
      IO {
        assertEquals(req.method, Method.GET)

        val expectedPath = s"/api/v2/feed/account/$testAccountUid/settled-transactions-between"
        assertEquals(req.uri.path.toString, expectedPath)

        // Check query parameters
        val queryParams = req.uri.query.params
        assertEquals(queryParams.get("minTransactionTimestamp"), Some(fmt.format(minTs)))
        assertEquals(queryParams.get("maxTransactionTimestamp"), Some(fmt.format(maxTs)))

        assertCommonHeaders(req)
      } *> Response[IO](Status.Ok).withEntity(sampleTxFeedResp).pure[IO]
    }

    withApi(httpApp) { api =>
      api.getSettledTransactionsBetween(testAccountUid, minTs, maxTs).map { response =>
        assertEquals(response, sampleTxFeedResp)
      }
    }
  }

  test("getSettledTransactionsBetween should handle empty transaction list") {
    val httpApp = HttpApp[IO] { req =>
      IO(assertCommonHeaders(req)) *> Response[IO](Status.Ok).withEntity(sampleTxFeedResp).pure[IO]
    }

    withApi(httpApp) { api =>
      api.getSettledTransactionsBetween(testAccountUid, minTs, maxTs).map { response =>
        assertEquals(response.feedItems, List.empty)
      }
    }
  }

  test("getSettledTransactionsBetween should format timestamps correctly") {
    val customMinTs = ZonedDateTime.parse("2023-12-25T10:30:45.123+01:00")
    val customMaxTs = ZonedDateTime.parse("2023-12-31T23:59:59.999-05:00")

    val httpApp = HttpApp[IO] { req =>
      IO {
        val queryParams = req.uri.query.params
        assertEquals(queryParams.get("minTransactionTimestamp"), Some(fmt.format(customMinTs)))
        assertEquals(queryParams.get("maxTransactionTimestamp"), Some(fmt.format(customMaxTs)))
        assertCommonHeaders(req)
      } *> Response[IO](Status.Ok).withEntity(sampleTxFeedResp).pure[IO]
    }

    withApi(httpApp) { api =>
      api.getSettledTransactionsBetween(testAccountUid, customMinTs, customMaxTs).map { response =>
        assertEquals(response.feedItems, List.empty)
      }
    }
  }

  test("getSettledTransactionsBetween should handle API errors") {
    val httpApp = HttpApp[IO](_ => Response[IO](Status.Forbidden).pure[IO])

    withApi(httpApp) { api =>
      api.getSettledTransactionsBetween(testAccountUid, minTs, maxTs).attempt.map { result =>
        assert(result.isLeft, "Should fail with HTTP error")
      }
    }
  }

  test("getSettledTransactionsBetween should handle malformed JSON response") {
    val httpApp = HttpApp[IO] { _ =>
      Response[IO](Status.Ok)
        .withEntity("invalid json")
        .withContentType(`Content-Type`(MediaType.application.json))
        .pure[IO]
    }

    withApi(httpApp) { api =>
      api.getSettledTransactionsBetween(testAccountUid, minTs, maxTs).attempt.map { result =>
        assert(result.isLeft, "Should fail with JSON parsing error")
      }
    }
  }

  test("getSettledTransactionsBetween should handle different account UIDs") {
    val differentAccountUid = UUID.randomUUID()

    val httpApp = HttpApp[IO] { req =>
      IO {
        val expectedPath = s"/api/v2/feed/account/$differentAccountUid/settled-transactions-between"
        assertEquals(req.uri.path.toString, expectedPath)
        assertCommonHeaders(req)
      } *> Response[IO](Status.Ok).withEntity(sampleTxFeedResp).pure[IO]
    }

    withApi(httpApp) { api =>
      api.getSettledTransactionsBetween(differentAccountUid, minTs, maxTs).map { response =>
        assertEquals(response, sampleTxFeedResp)
      }
    }
  }
}