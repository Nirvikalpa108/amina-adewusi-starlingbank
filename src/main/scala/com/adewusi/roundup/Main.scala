package com.adewusi.roundup

import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple {
  val run = RoundupServer.run[IO]
}
