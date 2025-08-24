package com.adewusi.roundup

import cats.effect.Concurrent
import com.adewusi.roundup.model.SavingsGoalsResponse
import org.http4s.Method._
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.Authorization
import org.http4s.implicits._
import org.typelevel.ci.CIStringSyntax

trait SavingsGoals[F[_]] {
  def getSavingsGoals(accountUid: String): F[SavingsGoalsResponse]
}

object SavingsGoals {
  val accessToken = "" //TODO get from config

  def impl[F[_]: Concurrent](C: Client[F]): SavingsGoals[F] = new SavingsGoals[F] {
    val dsl = new Http4sClientDsl[F] {}
    import dsl._

    override def getSavingsGoals(accountUid: String): F[SavingsGoalsResponse] = {
      C.expect[SavingsGoalsResponse](
        GET(
          uri"https://api-sandbox.starlingbank.com/api/v2/account" / accountUid / "savings-goals",
          Authorization(Credentials.Token(AuthScheme.Bearer, accessToken)),
          Header.Raw(ci"Accept", "application/json"),
          Header.Raw(ci"User-Agent", "Adewusi")
        )
      )
    }
  }
}