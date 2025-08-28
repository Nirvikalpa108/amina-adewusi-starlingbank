package com.adewusi.roundup.repository

import cats.effect.{IO, Ref}
import com.adewusi.roundup.model._
import munit.CatsEffectSuite

import java.util.UUID

class GoalRepositorySpec extends CatsEffectSuite {

  test("readGoal returns None when no initial goal is configured") {
    val config = AppConfig(
      StarlingConfig(
        accessToken = "test-token",
        baseUrl = "https://api.starlingbank.com",
        initialGoalId = None
      )
    )

    val result = for {
      goalRef <- Ref.of[IO, Option[UUID]](None)
      repo = GoalRepository.inMemoryGoalRepository[IO](goalRef)
      goal <- repo.readGoal(config)
    } yield goal

    assertIO(result, Right(None))
  }

  test("readGoal returns the initial goal ID from config") {
    val expectedGoalId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    val config = AppConfig(
      StarlingConfig(
        accessToken = "test-token",
        baseUrl = "https://api.starlingbank.com",
        initialGoalId = Some(expectedGoalId)
      )
    )

    val result = for {
      goalRef <- Ref.of[IO, Option[UUID]](Some(expectedGoalId))
      repo = GoalRepository.inMemoryGoalRepository[IO](goalRef)
      goal <- repo.readGoal(config)
    } yield goal

    assertIO(result, Right(Some(expectedGoalId)))
  }

  test("readGoal returns persisted goal after persistGoal is called") {
    val initialGoalId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    val newGoalId = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")

    val config = AppConfig(
      StarlingConfig(
        accessToken = "test-token",
        baseUrl = "https://api.starlingbank.com",
        initialGoalId = Some(initialGoalId)
      )
    )

    val result = for {
      goalRef <- Ref.of[IO, Option[UUID]](Some(initialGoalId))
      repo = GoalRepository.inMemoryGoalRepository[IO](goalRef)
      _ <- repo.persistGoal(newGoalId)
      goal <- repo.readGoal(config)
    } yield goal

    assertIO(result, Right(Some(newGoalId)))
  }
}