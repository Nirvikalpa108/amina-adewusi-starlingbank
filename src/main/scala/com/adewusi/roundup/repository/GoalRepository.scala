package com.adewusi.roundup.repository

import cats.effect.{Ref, Sync}
import cats.implicits._
import com.adewusi.roundup.model.AppError

import java.util.UUID

/**
  * Repository trait for managing a Goal identified by a UUID.
  *
  * @tparam F the effect type, typically a Cats Effect type like IO
  */
trait GoalRepository[F[_]] {
  /**
    * Reads the current goal UUID if it exists.
    *
    * @return an effectful computation yielding either an AppError or an optional UUID representing the goal
    */
  def readGoal: F[Either[AppError, Option[UUID]]]

  /**
    * Persists the given goal UUID.
    *
    * @param goal the UUID of the goal to persist
    * @return an effectful computation yielding either an AppError or Unit on success
    */
  def persistGoal(goal: UUID): F[Either[AppError, Unit]]
}

object GoalRepository {
  /**
    * Creates an in-memory implementation of GoalRepository using a Ref to hold state.
    *
    * @param goalRef a Cats Effect Ref holding an optional UUID representing the goal
    * @tparam F the effect type with Sync capability
    * @return a GoalRepository instance backed by in-memory state
    */
  def inMemoryGoalRepository[F[_]: Sync](goalRef: Ref[F, Option[UUID]]): GoalRepository[F] = {
    new GoalRepository[F] {
      override def readGoal: F[Either[AppError, Option[UUID]]] =
        goalRef.get.map(_.asRight[AppError])

      override def persistGoal(goal: UUID): F[Either[AppError, Unit]] =
        goalRef.set(Some(goal)).map(_.asRight[AppError])
    }
  }

  /**
    * Creates a dry-run implementation of GoalRepository that reads from in-memory state
    * but ignores writes (no-op on persist).
    *
    * Useful for testing or scenarios where persistence is disabled.
    *
    * @param goalRef a Cats Effect Ref holding an optional UUID representing the goal
    * @tparam F the effect type with Sync capability
    * @return a GoalRepository instance that does not persist changes
    */
  def dryRun[F[_]: Sync](goalRef: Ref[F, Option[UUID]]): GoalRepository[F] = new GoalRepository[F] {
    override def readGoal: F[Either[AppError, Option[UUID]]] = GoalRepository.inMemoryGoalRepository[F](goalRef).readGoal
    override def persistGoal(goal: UUID): F[Either[AppError, Unit]] = ().asRight[AppError].pure[F]
  }
}