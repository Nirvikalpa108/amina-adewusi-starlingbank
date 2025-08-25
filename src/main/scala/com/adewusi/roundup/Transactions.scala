package com.adewusi.roundup

import cats.effect.Concurrent
import com.adewusi.roundup.model.{AppConfig, TransactionFeedResponse}
import org.http4s.Method._
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.Authorization
import org.typelevel.ci.CIStringSyntax

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

trait Transactions[F[_]] {
  def getSettledTransactionsBetween(
      accountUid: String,
      minTransactionTimestamp: ZonedDateTime,
      maxTransactionTimestamp: ZonedDateTime
  ): F[TransactionFeedResponse]
}

object Transactions {
  def impl[F[_]: Concurrent](C: Client[F], config: AppConfig): Transactions[F] =
    new Transactions[F] {
      val dsl = new Http4sClientDsl[F] {}
      import dsl._

      override def getSettledTransactionsBetween(
          accountUid: String,
          minTransactionTimestamp: ZonedDateTime,
          maxTransactionTimestamp: ZonedDateTime
      ): F[TransactionFeedResponse] = {
        val min =
          minTransactionTimestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val max =
          maxTransactionTimestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        C.expect[TransactionFeedResponse](
          GET(
            Uri
              .unsafeFromString(
                s"https://api-sandbox.starlingbank.com/api/v2/feed/account/$accountUid/settled-transactions-between"
              )
              .withQueryParam("minTransactionTimestamp", min)
              .withQueryParam("maxTransactionTimestamp", max),
            Authorization(Credentials.Token(AuthScheme.Bearer, config.starling.accessToken)),
            Header.Raw(ci"Accept", "application/json"),
            Header.Raw(ci"User-Agent", "Adewusi")
          )
        )
      }
    }
}
