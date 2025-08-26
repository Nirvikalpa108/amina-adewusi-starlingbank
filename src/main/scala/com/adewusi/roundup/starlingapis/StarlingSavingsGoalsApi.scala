package com.adewusi.roundup.starlingapis

import cats.effect.Concurrent
import com.adewusi.roundup.model._
import org.http4s.Method._
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.Authorization
import org.http4s.implicits._
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
      C.expect[SavingsGoalsResponse](
        GET(
          uri"https://api-sandbox.starlingbank.com/api/v2/account" / accountUid / "savings-goals",
          Authorization(Credentials.Token(AuthScheme.Bearer, config.starling.accessToken)),
          Header.Raw(ci"Accept", "application/json"),
          Header.Raw(ci"User-Agent", "Adewusi")
        )
      )
    }

    override def createSavingsGoal(accountUid: String, request: CreateSavingsGoalRequest): F[CreateSavingsGoalResponse] = {
      C.expect[CreateSavingsGoalResponse](
        PUT(
          request,
          uri"https://api-sandbox.starlingbank.com/api/v2/account" / accountUid / "savings-goals",
          Authorization(Credentials.Token(AuthScheme.Bearer, config.starling.accessToken)),
          Header.Raw(ci"Accept", "application/json"),
          Header.Raw(ci"Content-Type", "application/json"),
          Header.Raw(ci"User-Agent", "Adewusi")
        )
      )
    }

    override def addMoney(accountUid: String, savingsGoalUid: String, transferUid: String, request: AddMoneyRequest): F[AddMoneyResponse] = {
      C.expect[AddMoneyResponse](
        PUT(
          request,
          uri"https://api-sandbox.starlingbank.com/api/v2/account" / accountUid / "savings-goals" / savingsGoalUid / "add-money" / transferUid,
          Authorization(Credentials.Token(AuthScheme.Bearer, config.starling.accessToken)),
          Header.Raw(ci"Accept", "application/json"),
          Header.Raw(ci"Content-Type", "application/json"),
          Header.Raw(ci"User-Agent", "Adewusi")
        )
      )
    }
  }
}