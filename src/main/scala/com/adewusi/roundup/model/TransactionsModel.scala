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
    direction: String,
    updatedAt: ZonedDateTime,
    transactionTime: ZonedDateTime,
    settlementTime: Option[ZonedDateTime],
    retryAllocationUntilTime: Option[ZonedDateTime],
    source: String,
    sourceSubType: Option[String],
    status: String,
    transactingApplicationUserUid: Option[UUID],
    counterPartyType: String,
    counterPartyUid: Option[UUID],
    counterPartyName: Option[String],
    counterPartySubEntityUid: Option[UUID],
    counterPartySubEntityName: Option[String],
    counterPartySubEntityIdentifier: Option[String],
    counterPartySubEntitySubIdentifier: Option[String],
    exchangeRate: Option[BigDecimal],
    totalFees: Option[BigDecimal],
    totalFeeAmount: Option[CurrencyAndAmount],
    reference: Option[String],
    country: String,
    spendingCategory: String,
    userNote: Option[String],
    roundUp: Option[AssociatedFeedRoundUp],
    hasAttachment: Option[Boolean],
    hasReceipt: Option[Boolean],
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
  implicit val batchPaymentDetailsEncoder: Encoder[BatchPaymentDetails] =
    deriveEncoder[BatchPaymentDetails]
  implicit val batchPaymentDetailsDecoder: Decoder[BatchPaymentDetails] =
    deriveDecoder[BatchPaymentDetails]
}

case class AssociatedFeedRoundUp(
    goalCategoryUid: UUID,
    amount: CurrencyAndAmount
)

object AssociatedFeedRoundUp {
  implicit val associatedFeedRoundUpDecoder: Decoder[AssociatedFeedRoundUp] =
    deriveDecoder[AssociatedFeedRoundUp]
  implicit val associatedFeedRoundUpEncoder: Encoder[AssociatedFeedRoundUp] =
    deriveEncoder[AssociatedFeedRoundUp]
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
