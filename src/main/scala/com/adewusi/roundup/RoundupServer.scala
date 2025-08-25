package com.adewusi.roundup

import cats.effect.Async
import cats.syntax.all._
import com.adewusi.roundup.model.AppConfig
import com.comcast.ip4s._
import fs2.io.net.Network
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger

object RoundupServer {

  def run[F[_]: Async: Network](config: AppConfig): F[Nothing] = {
    for {
      client <- EmberClientBuilder.default[F].build
      helloWorldAlg = HelloWorld.impl[F]
      jokeAlg = Jokes.impl[F](client)
      accountsAlg = Accounts.impl[F](client, config)
      transactionsAlg = Transactions.impl[F](client, config)
      savingsGoalsAlg = SavingsGoals.impl[F](client, config)

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract segments not checked
      // in the underlying routes.
      httpApp = (
        RoundupRoutes.helloWorldRoutes[F](helloWorldAlg) <+>
          RoundupRoutes.jokeRoutes[F](jokeAlg) <+>
          RoundupRoutes.accountsRoutes[F](accountsAlg) <+>
          RoundupRoutes.transactionsRoutes[F](transactionsAlg) <+>
          RoundupRoutes.savingsGoalsRoutes[F](savingsGoalsAlg)
      ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      _ <-
        EmberServerBuilder
          .default[F]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(finalHttpApp)
          .build
    } yield ()
  }.useForever
}
