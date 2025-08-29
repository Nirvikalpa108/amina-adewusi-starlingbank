package com.adewusi.roundup

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.adewusi.roundup.cli.Cli._

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    parseArgs(args) match {
      case Left(err) =>
        IO.println(s"❌ $err") *> printUsage.as(ExitCode.Error)

      case Right(cliArgs) =>
        val mode = if (cliArgs.isDryRun) "🔍 Dry run" else "🚀 Live run"
        IO.println(s"$mode starting from ${cliArgs.startDate}") *> {
          RoundupApp.run(cliArgs.startDate, cliArgs.isDryRun).flatMap {
            case Left(e) => IO.println(s"Error: $e")
            case Right(_) => IO.println("✅ Done")
          }
        }.as(ExitCode.Success)
    }
}
