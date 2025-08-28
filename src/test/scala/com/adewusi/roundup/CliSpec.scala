package com.adewusi.roundup

import com.adewusi.roundup.cli.Cli
import com.adewusi.roundup.model.CliArgs
import munit.FunSuite

import java.time.LocalDate
import java.util.UUID

class CliSpec extends FunSuite {

  private val sampleUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")

  test("parse minimal required args (start-date only)") {
    val args = List("--start-date", "2024-01-01")
    val result = Cli.parseArgs(args)

    assertEquals(result, Right(CliArgs(
      startDate = LocalDate.of(2024, 1, 1),
      dryRun = false,
      goalId = None
    )))
  }

  test("parse dry-run flag") {
    val args = List("--dry-run", "--start-date", "2024-01-01")
    val result = Cli.parseArgs(args)

    assertEquals(result, Right(CliArgs(
      startDate = LocalDate.of(2024, 1, 1),
      dryRun = true,
      goalId = None
    )))
  }

  test("parse with goal-id") {
    val args = List("--start-date", "2024-01-01", "--goal-id", sampleUuid.toString)
    val result = Cli.parseArgs(args)

    assertEquals(result, Right(CliArgs(
      startDate = LocalDate.of(2024, 1, 1),
      dryRun = false,
      goalId = Some(sampleUuid)
    )))
  }

  test("parse all options together") {
    val args = List("--dry-run", "--start-date", "2024-12-25", "--goal-id", sampleUuid.toString)
    val result = Cli.parseArgs(args)

    assertEquals(result, Right(CliArgs(
      startDate = LocalDate.of(2024, 12, 25),
      dryRun = true,
      goalId = Some(sampleUuid)
    )))
  }

  test("parse options in different order") {
    val args = List("--goal-id", sampleUuid.toString, "--dry-run", "--start-date", "2024-06-15")
    val result = Cli.parseArgs(args)

    assertEquals(result, Right(CliArgs(
      startDate = LocalDate.of(2024, 6, 15),
      dryRun = true,
      goalId = Some(sampleUuid)
    )))
  }

  test("fail when start-date is missing") {
    val args = List("--dry-run", "--goal-id", sampleUuid.toString)
    val result = Cli.parseArgs(args)

    assertEquals(result, Left("Missing required --start-date"))
  }

  test("fail with empty args") {
    val args = List.empty[String]
    val result = Cli.parseArgs(args)

    assertEquals(result, Left("Missing required --start-date"))
  }

  test("fail with unknown argument") {
    val args = List("--start-date", "2024-01-01", "--unknown-flag")
    val result = Cli.parseArgs(args)

    assert(result.isLeft)
    assert(result.left.exists(_.contains("Unknown argument: --unknown-flag")))
  }

  test("fail with invalid date format") {
    val args = List("--start-date", "01-01-2024")
    val result = Cli.parseArgs(args)

    assert(result.isLeft)
    // Should fail during LocalDate.parse
  }

  test("fail with invalid UUID format") {
    val args = List("--start-date", "2024-01-01", "--goal-id", "not-a-uuid")
    val result = Cli.parseArgs(args)

    assert(result.isLeft)
    // Should fail during UUID.fromString
  }

  test("fail when start-date has no value") {
    val args = List("--start-date")
    val result = Cli.parseArgs(args)

    assert(result.isLeft)
    // Should fail because there's no value after --start-date
  }

  test("fail when goal-id has no value") {
    val args = List("--start-date", "2024-01-01", "--goal-id")
    val result = Cli.parseArgs(args)

    assert(result.isLeft)
    // Should fail because there's no value after --goal-id
  }

  test("handle multiple dry-run flags (idempotent)") {
    val args = List("--dry-run", "--dry-run", "--start-date", "2024-01-01")
    val result = Cli.parseArgs(args)

    assertEquals(result, Right(CliArgs(
      startDate = LocalDate.of(2024, 1, 1),
      dryRun = true,
      goalId = None
    )))
  }

  test("handle leap year date") {
    val args = List("--start-date", "2024-02-29")
    val result = Cli.parseArgs(args)

    assertEquals(result, Right(CliArgs(
      startDate = LocalDate.of(2024, 2, 29),
      dryRun = false,
      goalId = None
    )))
  }

  test("fail with invalid leap year date") {
    val args = List("--start-date", "2023-02-29")
    val result = Cli.parseArgs(args)

    assert(result.isLeft)
    // 2023 is not a leap year, so Feb 29 should fail
  }
}