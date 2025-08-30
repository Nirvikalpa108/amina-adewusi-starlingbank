package com.adewusi.roundup.starlingapis

import cats.effect.Concurrent
import com.adewusi.roundup.model.TransactionFeedResponse
import org.http4s.Method._
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.Authorization
import org.typelevel.ci.CIStringSyntax

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

trait StarlingTransactionApi[F[_]] {
  def getSettledTransactionsBetween(
      accountUid: UUID,
      minTransactionTimestamp: ZonedDateTime,
      maxTransactionTimestamp: ZonedDateTime,
      accessToken: String,
      baseUri: Uri
  ): F[TransactionFeedResponse]
}

object StarlingTransactionApi {
  def impl[F[_]: Concurrent](
      C: Client[F]
  ): StarlingTransactionApi[F] =
    new StarlingTransactionApi[F] {
      val dsl = new Http4sClientDsl[F] {}
      import dsl._

      override def getSettledTransactionsBetween(
          accountUid: UUID,
          minTransactionTimestamp: ZonedDateTime,
          maxTransactionTimestamp: ZonedDateTime,
          accessToken: String,
          baseUri: Uri
      ): F[TransactionFeedResponse] = {
        val min =
          minTransactionTimestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val max =
          maxTransactionTimestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val requestUri =
          baseUri / "api" / "v2" / "feed" / "account" / accountUid.toString / "settled-transactions-between"

        C.expect[TransactionFeedResponse](
          GET(
            requestUri
              .withQueryParam("minTransactionTimestamp", min)
              .withQueryParam("maxTransactionTimestamp", max),
            Authorization(
              Credentials.Token(AuthScheme.Bearer, accessToken)
            ),
            Header.Raw(ci"Accept", "application/json"),
            Header.Raw(ci"User-Agent", "Adewusi")
          )
        )
      }
    }
}
