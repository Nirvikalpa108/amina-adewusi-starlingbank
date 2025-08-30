package com.adewusi.roundup.repository

import cats.effect.{IO, Ref}
import munit.CatsEffectSuite

import java.util.UUID

class GoalRepositorySpec extends CatsEffectSuite {

  private def makeRepo(initialGoal: Option[UUID]) = for {
    ref <- Ref.of[IO, Option[UUID]](initialGoal)
  } yield GoalRepository.inMemoryGoalRepository[IO](ref)

  test("readGoal returns None when no goal is set initially") {
    for {
      repo <- makeRepo(None)
      goal <- repo.readGoal
    } yield assertEquals(goal, Right(None))
  }

  test("readGoal returns the initial goal if configured") {
    val expectedGoalId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    for {
      repo <- makeRepo(Some(expectedGoalId))
      goal <- repo.readGoal
    } yield assertEquals(goal, Right(Some(expectedGoalId)))
  }

  test(
    "persistGoal updates the stored goal and readGoal returns the updated value"
  ) {
    val initialGoalId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    val newGoalId = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
    for {
      repo <- makeRepo(Some(initialGoalId))
      _ <- repo.persistGoal(newGoalId)
      goal <- repo.readGoal
    } yield assertEquals(goal, Right(Some(newGoalId)))
  }
}
