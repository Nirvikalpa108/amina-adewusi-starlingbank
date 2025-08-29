package com.adewusi.roundup.cli

import cats.effect.IO
import com.adewusi.roundup.model.CliArgs

import java.time.LocalDate
import java.util.UUID
import scala.util.{Failure, Success, Try}

object Cli {

  def parseArgs(args: List[String]): Either[String, CliArgs] = {
    // defaults
    var dry = false
    var goal: Option[UUID] = None
    var start: Option[LocalDate] = None

    def loop(rem: List[String]): Either[String, CliArgs] = rem match {
      case Nil =>
        start match {
          case Some(date) =>
            Right(CliArgs(startDate = date, isDryRun = dry, goalId = goal))
          case None =>
            Left("Missing required --start-date")
        }
      case "--dry-run" :: tail =>
        dry = true; loop(tail)
      case "--goal-id" :: v :: tail =>
        Try(UUID.fromString(v)) match {
          case Success(id) =>
            goal = Some(id); loop(tail)
          case Failure(_) =>
            Left(s"Invalid UUID format: $v")
        }
      case "--start-date" :: v :: tail =>
       Try(LocalDate.parse(v)) match {
          case Success(date) => start = Some(date); loop(tail)
          case Failure(_) => Left(s"Invalid date format: $v")
        }
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
