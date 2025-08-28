package com.adewusi.roundup.model

import cats.effect.Concurrent
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}

import java.util.UUID

final case class Account(
    accountUid: UUID,
    accountType: String,
    defaultCategory: UUID,
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
