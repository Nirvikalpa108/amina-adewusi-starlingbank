package com.adewusi.roundup.model

import cats.effect.Concurrent
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import cats.implicits._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

case class TransactionFeedItem(
    feedItemUid: UUID,
    categoryUid: UUID,
    amount: CurrencyAndAmount,
    sourceAmount: CurrencyAndAmount,
    direction: String, // IN or OUT
    updatedAt: ZonedDateTime,
    transactionTime: ZonedDateTime,
    settlementTime: Option[ZonedDateTime],
    source: String,
    status: String, // UPCOMING, PENDING, REVERSED, SETTLED, DECLINED, REFUNDED, RETRYING, ACCOUNT_CHECK
    counterPartyType: String,
    counterPartyName: Option[String],
    counterPartySubEntityName: Option[String],
    counterPartySubEntityIdentifier: Option[String],
    counterPartySubEntitySubIdentifier: Option[String],
    reference: Option[String],
    country: String,
    spendingCategory: String,
    hasAttachment: Boolean,
    hasReceipt: Boolean,
    batchPaymentDetails: Option[BatchPaymentDetails]
)

object TransactionFeedItem {

  // TODO do I need all of these low level encoder/decoders? Test.

  implicit val uuidDecoder: Decoder[UUID] =
    Decoder.decodeString.emap { s =>
      Either.catchNonFatal(UUID.fromString(s)).leftMap(_.getMessage)
    }
  implicit val uuidEncoder: Encoder[UUID] =
    Encoder.encodeString.contramap(_.toString)

  implicit val zonedDateTimeDecoder: Decoder[ZonedDateTime] =
    Decoder.decodeString.emap { s =>
      Either
        .catchNonFatal(ZonedDateTime.parse(s))
        .leftMap(_.getMessage)
    }
  implicit val zonedDateTimeEncoder: Encoder[ZonedDateTime] =
    Encoder.encodeString.contramap[ZonedDateTime](
      _.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    )

  implicit val transactionFeedItemDecoder: Decoder[TransactionFeedItem] =
    deriveDecoder
  implicit val transactionFeedItemEncoder: Encoder[TransactionFeedItem] =
    deriveEncoder
}

case class BatchPaymentDetails(
    batchPaymentUid: UUID,
    batchPaymentType: String
)

object BatchPaymentDetails {
  implicit val batchPaymentDetailsEncoder: Encoder[BatchPaymentDetails] = deriveEncoder[BatchPaymentDetails]
  implicit val batchPaymentDetailsDecoder: Decoder[BatchPaymentDetails] = deriveDecoder[BatchPaymentDetails]
}

case class TransactionFeedResponse(
    feedItems: List[TransactionFeedItem]
)

object TransactionFeedResponse {
  implicit val transactionFeedResponseDecoder
      : Decoder[TransactionFeedResponse] =
    deriveDecoder[TransactionFeedResponse]
  implicit val transactionFeedResponseEncoder
      : Encoder[TransactionFeedResponse] =
    deriveEncoder[TransactionFeedResponse]
  implicit def entityDecoder[F[_]: Concurrent]
      : EntityDecoder[F, TransactionFeedResponse] =
    jsonOf[F, TransactionFeedResponse]
}
