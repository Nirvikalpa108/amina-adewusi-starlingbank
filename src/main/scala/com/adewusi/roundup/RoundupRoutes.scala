package com.adewusi.roundup

import cats.effect.{Concurrent, Sync}
import cats.implicits._
import com.adewusi.roundup.model._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl

import java.time.ZonedDateTime

object RoundupRoutes {

  def jokeRoutes[F[_]: Sync](J: Jokes[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] { case GET -> Root / "joke" =>
      for {
        joke <- J.get
        resp <- Ok(joke)
      } yield resp
    }
  }

  def helloWorldRoutes[F[_]: Sync](H: HelloWorld[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] { case GET -> Root / "hello" / name =>
      for {
        greeting <- H.hello(HelloWorld.Name(name))
        resp <- Ok(greeting)
      } yield resp
    }
  }

  def accountsRoutes[F[_]: Sync](A: Accounts[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] { case GET -> Root / "accounts" =>
      (for {
        accounts <- A.getAccounts()
        resp <- Ok(accounts)
      } yield resp).handleErrorWith { e =>
        val rootCause = Option(e.getCause).getOrElse(e)
        Sync[F].delay(println(s"Error: ${e.getClass.getSimpleName}")) *>
          Sync[F].delay(
            println(
              s"Root cause: ${rootCause.getClass.getSimpleName}: ${rootCause.getMessage}"
            )
          ) *>
          Sync[F].delay(rootCause.printStackTrace()) *>
          InternalServerError("Internal error")
      }
    }
  }

  def transactionsRoutes[F[_]: Sync](T: Transactions[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    object MinTransactionTimestampQueryParamMatcher
        extends QueryParamDecoderMatcher[String]("minTransactionTimestamp")
    object MaxTransactionTimestampQueryParamMatcher
        extends QueryParamDecoderMatcher[String]("maxTransactionTimestamp")

    HttpRoutes.of[F] {
      case GET -> Root / "feed" / "account" / accountUid / "settled-transactions-between"
          :? MinTransactionTimestampQueryParamMatcher(minTimestampStr)
          +& MaxTransactionTimestampQueryParamMatcher(maxTimestampStr) =>
        for {
          // Parse timestamps
          minTs <- Sync[F].delay(ZonedDateTime.parse(minTimestampStr))
          maxTs <- Sync[F].delay(ZonedDateTime.parse(maxTimestampStr))
          transactions <- T.getSettledTransactionsBetween(
            accountUid,
            minTs,
            maxTs
          )
          resp <- Ok(transactions)
        } yield resp
    }
  }

  def savingsGoalsRoutes[F[_]: Concurrent](
      S: SavingsGoals[F]
  ): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "savings-goals" / accountUid =>
        for {
          savingsGoals <- S.getSavingsGoals(accountUid)
          resp <- Ok(savingsGoals)
        } yield resp

      case req @ PUT -> Root / "account" / accountUid / "savings-goals" =>
        (for {
          createReq <- req.as[CreateSavingsGoalRequest]
          created <- S.createSavingsGoal(accountUid, createReq)
          resp <- Ok(created)
        } yield resp).handleErrorWith {
          case _: org.http4s.MalformedMessageBodyFailure =>
            BadRequest("Invalid JSON")
          case e => InternalServerError(s"Internal error: ${e.getMessage}")
        }

      case req @ PUT -> Root /
          "account" / accountUid / "savings-goals" / savingsGoalUid / "add-money" / transferUid =>
        (for {
          addReq <- req.as[AddMoneyRequest]
          result <- S.addMoney(accountUid, savingsGoalUid, transferUid, addReq)
          resp <- Ok(result)
        } yield resp).handleErrorWith {
          case _: org.http4s.MalformedMessageBodyFailure =>
            BadRequest("Invalid JSON")
          case e => InternalServerError(s"Internal error: ${e.getMessage}")
        }
    }
  }
}
