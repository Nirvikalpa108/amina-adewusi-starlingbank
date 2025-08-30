package com.adewusi.roundup.model

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import org.http4s.Uri
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import pureconfig.generic.semiauto.deriveReader

import java.time.LocalDate
import java.util.UUID

case class StarlingConfig(
   accessToken: String,
   baseUri: Uri,
   initialGoalId: Option[UUID],
   startDate: Option[String] = None
)

object StarlingConfig {
  implicit val uriConfigReader: ConfigReader[Uri] = ConfigReader.fromString { str =>
    Uri.fromString(str).left.map(err => CannotConvert(str, "Uri", err.message))
  }
  implicit val starlingConfigReader: ConfigReader[StarlingConfig] = deriveReader[StarlingConfig]
}

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

final case class ZeroRoundup(reason: String) extends DomainError
case object NoAccount extends DomainError
final case class InvalidStoredSavingsGoalUuid(reason: String) extends DomainError
final case class AlreadyTransferred(reason: String) extends DomainError

final case class NotFoundError(reason: String) extends InfraError
final case class TransferError(reason: String) extends InfraError
final case class GenericError(reason: String) extends InfraError
final case class FileReadError(reason: String) extends InfraError
final case class FileWriteError(reason: String) extends InfraError

case class CliArgs(startDate: LocalDate, isDryRun: Boolean = false, goalId: Option[UUID])

final case class RoundupResult(amountMinorUnits: Long, goalId: UUID)