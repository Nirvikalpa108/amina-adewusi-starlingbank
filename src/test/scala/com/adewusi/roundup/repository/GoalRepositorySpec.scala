package com.adewusi.roundup.repository

import cats.effect.{IO, Ref}
import com.adewusi.roundup.TestUtils
import munit.CatsEffectSuite

import java.util.UUID

class GoalRepositorySpec extends CatsEffectSuite with TestUtils {

  test("readGoal returns None when no initial goal is configured") {
    val result = for {
      goalRef <- Ref.of[IO, Option[UUID]](None)
      repo = GoalRepository.inMemoryGoalRepository[IO](goalRef)
      goal <- repo.readGoal(testConfig)
    } yield goal

    assertIO(result, Right(None))
  }

  test("readGoal returns the initial goal ID from config") {
    val expectedGoalId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    val result = for {
      goalRef <- Ref.of[IO, Option[UUID]](Some(expectedGoalId))
      repo = GoalRepository.inMemoryGoalRepository[IO](goalRef)
      goal <- repo.readGoal(testConfig)
    } yield goal

    assertIO(result, Right(Some(expectedGoalId)))
  }

  test("readGoal returns persisted goal after persistGoal is called") {
    val initialGoalId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    val newGoalId = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
    val result = for {
      goalRef <- Ref.of[IO, Option[UUID]](Some(initialGoalId))
      repo = GoalRepository.inMemoryGoalRepository[IO](goalRef)
      _ <- repo.persistGoal(newGoalId)
      goal <- repo.readGoal(testConfig)
    } yield goal

    assertIO(result, Right(Some(newGoalId)))
  }
}