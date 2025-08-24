package com.adewusi.roundup

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl

import java.time.ZonedDateTime

object RoundupRoutes {

  def jokeRoutes[F[_]: Sync](J: Jokes[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "joke" =>
        for {
          joke <- J.get
          resp <- Ok(joke)
        } yield resp
    }
  }

  def helloWorldRoutes[F[_]: Sync](H: HelloWorld[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "hello" / name =>
        for {
          greeting <- H.hello(HelloWorld.Name(name))
          resp <- Ok(greeting)
        } yield resp
    }
  }

  def accountsRoutes[F[_]: Sync](A: Accounts[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "accounts" =>
        (for {
          accounts <- A.getAccounts()
          resp <- Ok(accounts)
        } yield resp).handleErrorWith { e =>
          val rootCause = Option(e.getCause).getOrElse(e)
          Sync[F].delay(println(s"Error: ${e.getClass.getSimpleName}")) *>
            Sync[F].delay(println(s"Root cause: ${rootCause.getClass.getSimpleName}: ${rootCause.getMessage}")) *>
            Sync[F].delay(rootCause.printStackTrace()) *>
            InternalServerError("Internal error")
        }
    }
  }

  def transactionsRoutes[F[_]: Sync](T: Transactions[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    object MinTransactionTimestampQueryParamMatcher extends QueryParamDecoderMatcher[String]("minTransactionTimestamp")
    object MaxTransactionTimestampQueryParamMatcher extends QueryParamDecoderMatcher[String]("maxTransactionTimestamp")

    HttpRoutes.of[F] {
      case GET -> Root / "feed" / "account" / accountUid / "settled-transactions-between"
        :? MinTransactionTimestampQueryParamMatcher(minTimestampStr)
        +& MaxTransactionTimestampQueryParamMatcher(maxTimestampStr) =>

        for {
          // Parse timestamps
          minTs <- Sync[F].delay(ZonedDateTime.parse(minTimestampStr))
          maxTs <- Sync[F].delay(ZonedDateTime.parse(maxTimestampStr))
          transactions <- T.getSettledTransactionsBetween(accountUid, minTs, maxTs)
          resp <- Ok(transactions)
        } yield resp
    }
  }

  def savingsGoalsRoutes[F[_]: Sync](S: SavingsGoals[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "accounts" / accountUid / "savings-goals" =>
        for {
          savingsGoals <- S.getSavingsGoals(accountUid)
          resp <- Ok(savingsGoals)
        } yield resp
    }
  }
}