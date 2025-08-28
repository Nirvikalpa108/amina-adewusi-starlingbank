package com.adewusi.roundup.model

import cats.effect.Concurrent
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}

import java.util.UUID

case class SavingsGoal(
    savingsGoalUid: UUID,
    name: String,
    target: Option[CurrencyAndAmount],
    totalSaved: CurrencyAndAmount,
    savedPercentage: Option[Int],
    state: String
)

object SavingsGoal {
  implicit val savingsGoalEncoder: Encoder[SavingsGoal] =
    deriveEncoder[SavingsGoal]
  implicit val savingsGoalDecoder: Decoder[SavingsGoal] =
    deriveDecoder[SavingsGoal]
}

case class SavingsGoalsResponse(
    savingsGoalList: List[SavingsGoal]
)

object SavingsGoalsResponse {
  implicit val savingsGoalsResponseEncoder: Encoder[SavingsGoalsResponse] =
    deriveEncoder
  implicit val savingsGoalsResponseDecoder: Decoder[SavingsGoalsResponse] =
    deriveDecoder

  implicit def entityDecoder[F[_]: Concurrent]
      : EntityDecoder[F, SavingsGoalsResponse] =
    jsonOf[F, SavingsGoalsResponse]
}

case class CreateSavingsGoalRequest(
    name: String,
    currency: String,
    target: Option[CurrencyAndAmount] = None,
    base64EncodedPhoto: Option[String] = None
)

object CreateSavingsGoalRequest {
  implicit val createSavingsGoalRequestEncoder
      : Encoder[CreateSavingsGoalRequest] = deriveEncoder
  implicit val createSavingsGoalRequestDecoder
      : Decoder[CreateSavingsGoalRequest] = deriveDecoder

  implicit def entityEncoder[F[_]]: EntityEncoder[F, CreateSavingsGoalRequest] =
    jsonEncoderOf[F, CreateSavingsGoalRequest]
  implicit def entityDecoder[F[_]: Concurrent]
      : EntityDecoder[F, CreateSavingsGoalRequest] =
    jsonOf[F, CreateSavingsGoalRequest]
}

case class CreateSavingsGoalResponse(
    savingsGoalUid: UUID,
    success: Boolean
)

object CreateSavingsGoalResponse {
  implicit val createSavingsGoalResponseEncoder
      : Encoder[CreateSavingsGoalResponse] =
    deriveEncoder[CreateSavingsGoalResponse]
  implicit val createSavingsGoalResponseDecoder
      : Decoder[CreateSavingsGoalResponse] =
    deriveDecoder[CreateSavingsGoalResponse]

  implicit def entityEncoder[F[_]]
      : EntityEncoder[F, CreateSavingsGoalResponse] =
    jsonEncoderOf[F, CreateSavingsGoalResponse]
  implicit def entityDecoder[F[_]: Concurrent]
      : EntityDecoder[F, CreateSavingsGoalResponse] =
    jsonOf[F, CreateSavingsGoalResponse]
}

final case class Reference private (value: String)

object Reference {
  def fromString(s: String): Either[String, Reference] =
    if (s.length <= 100) Right(Reference(s))
    else Left(s"Reference too long: ${s.length} chars")

  implicit val referenceEncoder: Encoder[Reference] =
    Encoder.encodeString.contramap(_.value)

  implicit val referenceDecoder: Decoder[Reference] =
    Decoder.decodeString.emap(fromString)
}

case class AddMoneyRequest(
    amount: CurrencyAndAmount,
    reference: Option[Reference] = None
)

object AddMoneyRequest {
  implicit val addMoneyRequestDecoder: Decoder[AddMoneyRequest] = deriveDecoder
  implicit val addMoneyRequestEncoder: Encoder[AddMoneyRequest] = deriveEncoder

  implicit def entityEncoder[F[_]]: EntityEncoder[F, AddMoneyRequest] =
    jsonEncoderOf[F, AddMoneyRequest]

  implicit def entityDecoder[F[_]: Concurrent]
      : EntityDecoder[F, AddMoneyRequest] =
    jsonOf[F, AddMoneyRequest]
}

case class AddMoneyResponse(
    transferUid: UUID,
    success: Boolean
)

object AddMoneyResponse {
  implicit val addMoneyResponseDecoder: Decoder[AddMoneyResponse] =
    deriveDecoder
  implicit val addMoneyResponseEncoder: Encoder[AddMoneyResponse] =
    deriveEncoder

  implicit def entityEncoder[F[_]]: EntityEncoder[F, AddMoneyResponse] =
    jsonEncoderOf[F, AddMoneyResponse]

  implicit def entityDecoder[F[_]: Concurrent]
      : EntityDecoder[F, AddMoneyResponse] =
    jsonOf[F, AddMoneyResponse]
}
