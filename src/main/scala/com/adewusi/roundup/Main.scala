package com.adewusi.roundup

import cats.effect.kernel.Ref
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.adewusi.roundup.cli.Cli._
import com.adewusi.roundup.model.TransferRecord

import java.util.UUID

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    parseArgs(args) match {
      case Left(err) =>
        IO.println(s"❌ $err") *> printUsage.as(ExitCode.Error)

      case Right(cliArgs) =>
        val mode = if (cliArgs.isDryRun) "🔍 Dry run" else "🚀 Live run"
        IO.println(s"$mode starting from ${cliArgs.startDate}") *> {
          for {
            transferRef <- Ref.of[IO, Set[TransferRecord]](Set.empty)
            goalRef <- Ref.of[IO, Option[UUID]](None)
          } yield {
            RoundupApp.run(cliArgs.startDate, cliArgs.isDryRun, goalRepoRef = goalRef, transferRepoRef = transferRef).flatMap {
              case Left(e) => IO.println(s"Error: $e")
              case Right(result) => IO.println(s"✅ Done - ${result.amountMinorUnits}p transferred to savings goal UUID ${result.goalId}")
            }
          }
        }.as(ExitCode.Success)
    }
}
