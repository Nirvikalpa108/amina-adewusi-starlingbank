package com.adewusi.roundup

import cats.effect.Concurrent
import com.adewusi.roundup.model.{AccountsResponse, AppConfig}
import org.http4s.Method._
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.Authorization
import org.http4s.implicits._
import org.typelevel.ci.CIStringSyntax

trait Accounts[F[_]] {
  def getAccounts(): F[AccountsResponse]
}

object Accounts {

  def impl[F[_]: Concurrent](C: Client[F], config: AppConfig): Accounts[F] = new Accounts[F] {
    val dsl = new Http4sClientDsl[F] {}
    import dsl._
    override def getAccounts(): F[AccountsResponse] = {
      C.expect[AccountsResponse](
        GET(
          uri"https://api-sandbox.starlingbank.com/api/v2/accounts",
          Authorization(Credentials.Token(AuthScheme.Bearer, config.starling.accessToken)),
          Header.Raw(ci"Accept", "application/json"),
          Header.Raw(ci"User-Agent", "Adewusi")
        )
      )
    }
  }
}
