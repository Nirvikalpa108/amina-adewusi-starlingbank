package com.adewusi.roundup.model

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

case class StarlingConfig(
    accessToken: String,
    baseUrl: String
)

case class AppConfig(
    starling: StarlingConfig
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

sealed trait AppError
sealed trait DomainError extends AppError
sealed trait InfraError extends AppError

case object NoTransactions extends DomainError
case object ZeroRoundupAmount extends DomainError
final case class AlreadyTransferred(reason: String) extends DomainError

final case class NotFoundError(reason: String) extends InfraError
final case class TransferError(reason: String) extends InfraError
final case class GenericError(reason: String) extends InfraError
