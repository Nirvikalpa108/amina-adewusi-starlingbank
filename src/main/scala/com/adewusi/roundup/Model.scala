package com.adewusi.roundup

import cats.effect.Concurrent
import cats.implicits._
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import org.http4s._
import org.http4s.circe._

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

final case class Account(
    accountUid: String,
    accountType: String,
    defaultCategory: String,
    currency: String,
    createdAt: String,
    name: String
)

object Account {
  implicit val accountDecoder: Decoder[Account] = deriveDecoder[Account]
  implicit val accountEncoder: Encoder[Account] = deriveEncoder[Account]
}

final case class AccountsResponse(accounts: List[Account])

object AccountsResponse {
  implicit val accountsResponseDecoder: Decoder[AccountsResponse] =
    deriveDecoder[AccountsResponse]
  implicit val accountsResponseEncoder: Encoder[AccountsResponse] =
    deriveEncoder[AccountsResponse]

  implicit def accountsResponseEntityDecoder[F[_]: Concurrent]
      : EntityDecoder[F, AccountsResponse] =
    jsonOf[F, AccountsResponse]
  implicit def accountsResponseEntityEncoder[F[_]]
      : EntityEncoder[F, AccountsResponse] =
    jsonEncoderOf[F, AccountsResponse]
}

final case class StarlingError(e: Throwable) extends RuntimeException

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

case class CurrencyAndAmount(
    currency: String,
    minorUnits: Long
)

object CurrencyAndAmount {
  implicit val currencyAndAmountDecoder: Decoder[CurrencyAndAmount] =
    deriveDecoder
  implicit val currencyAndAmountEncoder: Encoder[CurrencyAndAmount] =
    deriveEncoder
}

case class BatchPaymentDetails(
    batchPaymentUid: UUID,
    batchPaymentType: String
)

case class TransactionFeedResponse(
    feedItems: List[TransactionFeedItem]
)

object TransactionFeedResponse {

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

  implicit val batchPaymentDetailsDecoder: Decoder[BatchPaymentDetails] =
    deriveDecoder
  implicit val batchPaymentDetailsEncoder: Encoder[BatchPaymentDetails] =
    deriveEncoder

  implicit val transactionFeedItemDecoder: Decoder[TransactionFeedItem] =
    deriveDecoder
  implicit val transactionFeedItemEncoder: Encoder[TransactionFeedItem] =
    deriveEncoder

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

case class SavingsGoal(
    savingsGoalUid: UUID,
    name: String,
    target: CurrencyAndAmount,
    totalSaved: CurrencyAndAmount,
    savedPercentage: Int,
    state: String = "ACTIVE"
)

object SavingsGoal {
  implicit val savingsGoalEncoder: Encoder[SavingsGoal] = deriveEncoder
  implicit val savingsGoalDecoder: Decoder[SavingsGoal] = deriveDecoder

  implicit def entityDecoder[F[_]: Concurrent]: EntityDecoder[F, SavingsGoal] =
    jsonOf[F, SavingsGoal]
}

case class SavingsGoalsResponse(
    savingsGoalList: List[SavingsGoal]
)

object SavingsGoalsResponse {
  implicit val savingsGoalsResponseEncoder: Encoder[SavingsGoalsResponse] = deriveEncoder
  implicit val savingsGoalsResponseDecoder: Decoder[SavingsGoalsResponse] = deriveDecoder

  implicit def entityDecoder[F[_]: Concurrent]: EntityDecoder[F, SavingsGoalsResponse] =
    jsonOf[F, SavingsGoalsResponse]
}

