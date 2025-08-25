package com.adewusi.roundup

import cats.effect.{IO, IOApp}
import com.adewusi.roundup.model.AppConfig
import pureconfig._
import pureconfig.generic.auto._

object Main extends IOApp.Simple {
  val run: IO[Unit] = load.flatMap((config => RoundupServer.run[IO](config)))

  private def load: IO[AppConfig] = {
    IO.fromEither(
      ConfigSource.default
        .load[AppConfig]
        .left
        .map(failures =>
          new RuntimeException(
            s"Failed to load config: ${failures.toList.mkString(", ")}"
          )
        )
    )
  }
}
