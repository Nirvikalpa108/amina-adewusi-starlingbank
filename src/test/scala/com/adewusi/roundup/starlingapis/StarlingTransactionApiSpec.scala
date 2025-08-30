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

  private val testAccountUid: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
  private val differentAccountUid: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")

  private val sampleTxFeedResp: TransactionFeedResponse =
    TransactionFeedResponse(feedItems = List.empty)

  private val minTs: ZonedDateTime = ZonedDateTime.parse("2023-08-01T00:00:00Z")
  private val maxTs: ZonedDateTime = ZonedDateTime.parse("2023-08-08T23:59:59Z")
  private val fmt: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  private def withApi(httpApp: HttpApp[IO])(f: StarlingTransactionApi[IO] => IO[Unit]): IO[Unit] = {
    val client = Client.fromHttpApp(httpApp)
    val api = StarlingTransactionApi.impl[IO](client)
    f(api)
  }

  private def assertCommonHeaders(request: Request[IO]): IO[Unit] = IO {
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

  test("getSettledTransactionsBetween returns transactions on successful API call") {
    val httpApp = HttpApp[IO] { req =>
      for {
        _ <- IO(assertEquals(req.method, Method.GET))
        _ <- IO(assertEquals(req.uri.path.toString, s"/api/v2/feed/account/$testAccountUid/settled-transactions-between"))
        _ <- IO {
          val queryParams = req.uri.query.params
          assertEquals(queryParams.get("minTransactionTimestamp"), Some(fmt.format(minTs)))
          assertEquals(queryParams.get("maxTransactionTimestamp"), Some(fmt.format(maxTs)))
        }
        _ <- assertCommonHeaders(req)
        resp <- Response[IO](Status.Ok).withEntity(sampleTxFeedResp).pure[IO]
      } yield resp
    }

    withApi(httpApp) { api =>
      api.getSettledTransactionsBetween(testAccountUid, minTs, maxTs, testConfig.starling.accessToken, testConfig.starling.baseUri).flatMap { response =>
        IO(assertEquals(response, sampleTxFeedResp))
      }
    }
  }

  test("getSettledTransactionsBetween returns empty list when no transactions") {
    val httpApp = HttpApp[IO] { req =>
      assertCommonHeaders(req) *> Response[IO](Status.Ok).withEntity(sampleTxFeedResp).pure[IO]
    }

    withApi(httpApp) { api =>
      api.getSettledTransactionsBetween(testAccountUid, minTs, maxTs, testConfig.starling.accessToken, testConfig.starling.baseUri).flatMap { response =>
        IO(assertEquals(response.feedItems, List.empty))
      }
    }
  }

  test("getSettledTransactionsBetween formats timestamps correctly with custom timezones") {
    val customMinTs = ZonedDateTime.parse("2023-12-25T10:30:45.123+01:00")
    val customMaxTs = ZonedDateTime.parse("2023-12-31T23:59:59.999-05:00")

    val httpApp = HttpApp[IO] { req =>
      for {
        _ <- IO {
          val queryParams = req.uri.query.params
          assertEquals(queryParams.get("minTransactionTimestamp"), Some(fmt.format(customMinTs)))
          assertEquals(queryParams.get("maxTransactionTimestamp"), Some(fmt.format(customMaxTs)))
        }
        _ <- assertCommonHeaders(req)
        resp <- Response[IO](Status.Ok).withEntity(sampleTxFeedResp).pure[IO]
      } yield resp
    }

    withApi(httpApp) { api =>
      api.getSettledTransactionsBetween(testAccountUid, customMinTs, customMaxTs, testConfig.starling.accessToken, testConfig.starling.baseUri).flatMap { response =>
        IO(assertEquals(response.feedItems, List.empty))
      }
    }
  }

  test("getSettledTransactionsBetween fails with Forbidden status") {
    val httpApp = HttpApp[IO](_ => Response[IO](Status.Forbidden).pure[IO])

    withApi(httpApp) { api =>
      api.getSettledTransactionsBetween(testAccountUid, minTs, maxTs, testConfig.starling.accessToken, testConfig.starling.baseUri).attempt.flatMap {
        case Left(e) =>
          IO {
            assert(e.getMessage.contains("403") || e.getMessage.toLowerCase.contains("forbidden"))
          }
        case Right(_) =>
          IO(fail("Expected failure but got success"))
      }
    }
  }

  test("getSettledTransactionsBetween fails on malformed JSON response") {
    val httpApp = HttpApp[IO] { _ =>
      Response[IO](Status.Ok)
        .withEntity("invalid json")
        .withContentType(`Content-Type`(MediaType.application.json))
        .pure[IO]
    }

    withApi(httpApp) { api =>
      api.getSettledTransactionsBetween(testAccountUid, minTs, maxTs, testConfig.starling.accessToken, testConfig.starling.baseUri).attempt.flatMap {
        case Left(e) =>
          IO {
            assert(e.getMessage.toLowerCase.contains("json") || e.getMessage.toLowerCase.contains("parsing"))
          }
        case Right(_) =>
          IO(fail("Expected JSON parsing failure but got success"))
      }
    }
  }

  test("getSettledTransactionsBetween works with different account UIDs") {
    val httpApp = HttpApp[IO] { req =>
      for {
        _ <- IO(assertEquals(req.uri.path.toString, s"/api/v2/feed/account/$differentAccountUid/settled-transactions-between"))
        _ <- assertCommonHeaders(req)
        resp <- Response[IO](Status.Ok).withEntity(sampleTxFeedResp).pure[IO]
      } yield resp
    }

    withApi(httpApp) { api =>
      api.getSettledTransactionsBetween(differentAccountUid, minTs, maxTs, testConfig.starling.accessToken, testConfig.starling.baseUri).flatMap { response =>
        IO(assertEquals(response, sampleTxFeedResp))
      }
    }
  }
}