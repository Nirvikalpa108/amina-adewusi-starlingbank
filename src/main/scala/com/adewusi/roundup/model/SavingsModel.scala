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
  implicit val savingsGoalEncoder: Encoder[SavingsGoal] = deriveEncoder[SavingsGoal]
  implicit val savingsGoalDecoder: Decoder[SavingsGoal] = deriveDecoder[SavingsGoal]
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

case class ErrorDetail(
    message: String
)

object ErrorDetail {
  implicit val errorDetailEncoder: Encoder[ErrorDetail] = deriveEncoder[ErrorDetail]
  implicit val errorDetailDecoder: Decoder[ErrorDetail] = deriveDecoder[ErrorDetail]
}

case class CreateSavingsGoalResponse(
    savingsGoalUid: UUID,
    success: Boolean,
    errors: List[ErrorDetail] = List.empty
)

object CreateSavingsGoalResponse {
  implicit val createSavingsGoalResponseEncoder
      : Encoder[CreateSavingsGoalResponse] = deriveEncoder
  implicit val createSavingsGoalResponseDecoder
      : Decoder[CreateSavingsGoalResponse] = deriveDecoder

  implicit def entityDecoder[F[_]: Concurrent]
      : EntityDecoder[F, CreateSavingsGoalResponse] =
    jsonOf[F, CreateSavingsGoalResponse]
}
