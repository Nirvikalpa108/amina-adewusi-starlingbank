package com.adewusi.roundup

import cats.effect.IO
import com.adewusi.roundup.model.AppConfig
import pureconfig._
import pureconfig.generic.auto._

object Config {
  def load: IO[AppConfig] = {
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
