package com.adewusi.roundup.starlingapis

import cats.effect.Concurrent
import com.adewusi.roundup.model.AccountsResponse
import org.http4s.Method.GET
import org.http4s.{AuthScheme, Credentials, _}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.Authorization
import org.typelevel.ci.CIStringSyntax

trait StarlingAccountsApi[F[_]] {
  def getAccounts(accessToken: String, baseUri: Uri): F[AccountsResponse]
}

object StarlingAccountsApi {

  def impl[F[_]: Concurrent](C: Client[F]): StarlingAccountsApi[F] = new StarlingAccountsApi[F] {
    val dsl = new Http4sClientDsl[F] {}
    import dsl._
    override def getAccounts(accessToken: String, baseUri: Uri): F[AccountsResponse] = {
      val requestUri = baseUri / "api" / "v2" / "accounts"
      C.expect[AccountsResponse](
        GET(
          requestUri,
          Authorization(Credentials.Token(AuthScheme.Bearer, accessToken)),
          Header.Raw(ci"Accept", "application/json"),
          Header.Raw(ci"User-Agent", "Adewusi")
        )
      )
    }
  }
}
