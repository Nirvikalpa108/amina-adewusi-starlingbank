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
        IO.println(s"error: $err") *> printUsage.as(ExitCode.Error)

      case Right(cliArgs) =>
        for {
          config <- Config.load
          _ <- IO.println(s"Start date: ${cliArgs.startDate}")
          _ <- IO.println(s"Dry run mode: ${cliArgs.isDryRun}")
          _ <- IO.println(s"Goal ID: ${cliArgs.goalId.getOrElse("none")}")
          goalRef <- Ref.of[IO, Option[UUID]](cliArgs.goalId)
          transferRef <- Ref.of[IO, Set[TransferRecord]](Set.empty)
          result <- RoundupApp.run(
            startDate = cliArgs.startDate,
            isDryRun = cliArgs.isDryRun,
            goalRepoRef = goalRef,
            transferRepoRef = transferRef,
            goalId = cliArgs.goalId,
            config = config
          )
          _ <- result match {
            case Left(e)  => IO.println(s"error: Error: $e")
            case Right(r) => IO.println(s"Roundup complete (Dry run mode: ${cliArgs.isDryRun}) - ${r.amountMinorUnits}p transferred to savings goal UUID ${r.goalId}")
          }
        } yield ExitCode.Success
    }
}
