package com.adewusi.roundup.starlingapis

import cats.effect.IO
import com.adewusi.roundup.RoundupRoutes
import com.adewusi.roundup.model.TransactionFeedResponse
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.implicits._

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class StarlingTransactionApiRoutesSpec extends CatsEffectSuite {

  private val sampleTxFeedResp: TransactionFeedResponse =
    TransactionFeedResponse(feedItems = List.empty)

  private val minTs: ZonedDateTime = ZonedDateTime.parse("2023-08-01T00:00:00Z")
  private val maxTs: ZonedDateTime = ZonedDateTime.parse("2023-08-08T23:59:59Z")
  private val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  test(
    "GET /feed/account/{accountUid}/settled-transactions-between returns status code 200"
  ) {
    assertIO(retTransactionsSuccess.map(_.status), Status.Ok)
  }

  test(
    "GET /feed/account/{accountUid}/settled-transactions-between returns TransactionFeedResponse JSON"
  ) {
    assertIO(
      retTransactionsSuccess.flatMap(_.as[TransactionFeedResponse]),
      sampleTxFeedResp
    )
  }

  test(
    "GET /feed/account/{accountUid}/settled-transactions-between returns 400 Bad Request for invalid timestamp"
  ) {
    assertIO(retTransactionsInvalidTimestamp.map(_.status), Status.BadRequest)
  }

  test(
    "GET /feed/account/{accountUid}/settled-transactions-between returns 404 Bad Request for missing query params"
  ) {
    assertIO(retTransactionsMissingParams.map(_.status), Status.NotFound)
  }

  private[this] val retTransactionsSuccess: IO[Response[IO]] = {
    val minStr = minTs.format(fmt)
    val maxStr = maxTs.format(fmt)
    val req = Request[IO](
      Method.GET,
      uri"/feed/account/85A33967-C9C7-4803-97FF-36EDE76343FA/settled-transactions-between"
        .withQueryParam("minTransactionTimestamp", minStr)
        .withQueryParam("maxTransactionTimestamp", maxStr)
    )

    val transactions = new StarlingTransactionApi[IO] {
      override def getSettledTransactionsBetween(
          accountUid: UUID,
          minTransactionTimestamp: ZonedDateTime,
          maxTransactionTimestamp: ZonedDateTime
      ): IO[TransactionFeedResponse] =
        IO.pure(sampleTxFeedResp)
    }

    RoundupRoutes.transactionsRoutes[IO](transactions).orNotFound(req)
  }

  private[this] val retTransactionsInvalidTimestamp: IO[Response[IO]] = {
    val req = Request[IO](
      Method.GET,
      uri"/feed/account/85A33967-C9C7-4803-97FF-36EDE76343FA/settled-transactions-between"
        .withQueryParam("minTransactionTimestamp", "invalid-timestamp")
        .withQueryParam("maxTransactionTimestamp", "2023-08-08T23:59:59Z")
    )

    val transactions = new StarlingTransactionApi[IO] {
      override def getSettledTransactionsBetween(
          accountUid: UUID,
          minTransactionTimestamp: ZonedDateTime,
          maxTransactionTimestamp: ZonedDateTime
      ): IO[TransactionFeedResponse] =
        IO.pure(sampleTxFeedResp)
    }

    RoundupRoutes.transactionsRoutes[IO](transactions).orNotFound(req)
  }

  private[this] val retTransactionsMissingParams: IO[Response[IO]] = {
    val req = Request[IO](
      Method.GET,
      uri"/feed/account/85A33967-C9C7-4803-97FF-36EDE76343FA/settled-transactions-between"
      // Missing query parameters
    )

    val transactions = new StarlingTransactionApi[IO] {
      override def getSettledTransactionsBetween(
          accountUid: UUID,
          minTransactionTimestamp: ZonedDateTime,
          maxTransactionTimestamp: ZonedDateTime
      ): IO[TransactionFeedResponse] =
        IO.pure(sampleTxFeedResp)
    }

    RoundupRoutes.transactionsRoutes[IO](transactions).orNotFound(req)
  }
}
