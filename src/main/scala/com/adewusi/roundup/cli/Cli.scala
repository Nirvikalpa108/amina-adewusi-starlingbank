package com.adewusi.roundup.cli

import cats.effect.IO
import com.adewusi.roundup.model.CliArgs

import java.time.LocalDate
import java.util.UUID

object Cli {

  def parseArgs(args: List[String]): Either[String, CliArgs] = {
    // defaults
    var dry = true
    var goal: Option[UUID] = None
    var start: Option[LocalDate] = None

    def loop(rem: List[String]): Either[String, CliArgs] = rem match {
      case Nil =>
        start match {
          case Some(date) => Right(CliArgs(date, dry, goal))
          case None       => Left("Missing required --start-date")
        }
      case "--dry-run" :: tail =>
        dry = true; loop(tail)
      case "--goal-id" :: v :: tail =>
        goal = Some(UUID.fromString(v)); loop(tail)
      case "--start-date" :: v :: tail =>
        start = Some(LocalDate.parse(v)); loop(tail)
      case x :: _ =>
        Left(s"Unknown argument: $x")
    }

    loop(args)
  }

  def printUsage: IO[Unit] =
    IO.println(
      """Usage:
        |  sbt "run --start-date YYYY-MM-DD [--goal-id <uuid>] [--dry-run]"
        |""".stripMargin
    )
}
