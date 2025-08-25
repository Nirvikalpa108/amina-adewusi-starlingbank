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
