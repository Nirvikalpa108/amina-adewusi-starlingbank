package com.adewusi.roundup

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

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
}