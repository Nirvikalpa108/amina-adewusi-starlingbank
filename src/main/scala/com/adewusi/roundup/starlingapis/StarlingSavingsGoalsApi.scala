package com.adewusi.roundup.starlingapis

import cats.effect.Concurrent
import com.adewusi.roundup.model._
import org.http4s.Method._
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.Authorization
import org.typelevel.ci.CIStringSyntax

trait StarlingSavingsGoalsApi[F[_]] {
  def getSavingsGoals(accountUid: String): F[SavingsGoalsResponse]
  def createSavingsGoal(accountUid: String, request: CreateSavingsGoalRequest): F[CreateSavingsGoalResponse]
  def addMoney(accountUid: String, savingsGoalUid: String, transferUid: String, request: AddMoneyRequest): F[AddMoneyResponse]
}

object StarlingSavingsGoalsApi {
  def impl[F[_]: Concurrent](C: Client[F], config: AppConfig): StarlingSavingsGoalsApi[F] = new StarlingSavingsGoalsApi[F] {
    val dsl = new Http4sClientDsl[F] {}
    import dsl._

    override def getSavingsGoals(accountUid: String): F[SavingsGoalsResponse] = {
      val requestUri = config.starling.baseUri / "api" / "v2" / "account" / accountUid / "savings-goals"
      C.expect[SavingsGoalsResponse](
        GET(
          requestUri,
          Authorization(Credentials.Token(AuthScheme.Bearer, config.starling.accessToken)),
          Header.Raw(ci"Accept", "application/json"),
          Header.Raw(ci"User-Agent", "Adewusi")
        )
      )
    }

    override def createSavingsGoal(accountUid: String, request: CreateSavingsGoalRequest): F[CreateSavingsGoalResponse] = {
      val requestUri = config.starling.baseUri / "api" / "v2" / "account" / accountUid / "savings-goals"
      C.expect[CreateSavingsGoalResponse](
        PUT(
          request,
          requestUri,
          Authorization(Credentials.Token(AuthScheme.Bearer, config.starling.accessToken)),
          Header.Raw(ci"Accept", "application/json"),
          Header.Raw(ci"Content-Type", "application/json"),
          Header.Raw(ci"User-Agent", "Adewusi")
        )
      )
    }

    override def addMoney(accountUid: String, savingsGoalUid: String, transferUid: String, request: AddMoneyRequest): F[AddMoneyResponse] = {
      val requestUri = config.starling.baseUri / "api" / "v2" / "account" / accountUid / "savings-goals" / savingsGoalUid / "add-money" / transferUid
      C.expect[AddMoneyResponse](
        PUT(
          request,
          requestUri,
          Authorization(Credentials.Token(AuthScheme.Bearer, config.starling.accessToken)),
          Header.Raw(ci"Accept", "application/json"),
          Header.Raw(ci"Content-Type", "application/json"),
          Header.Raw(ci"User-Agent", "Adewusi")
        )
      )
    }
  }
}