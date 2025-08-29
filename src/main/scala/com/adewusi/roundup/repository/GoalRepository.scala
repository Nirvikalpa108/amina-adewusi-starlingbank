package com.adewusi.roundup.repository

import com.adewusi.roundup.model.{AppConfig, AppError}

import cats.effect.{Ref, Sync}
import cats.implicits._
import java.util.UUID

trait GoalRepository[F[_]] {
  def readGoal(config: AppConfig): F[Either[AppError, Option[UUID]]]
  def persistGoal(goal: UUID): F[Either[AppError, Unit]]
}

object GoalRepository {
  def inMemoryGoalRepository[F[_]: Sync](goalRef: Ref[F, Option[UUID]]): GoalRepository[F] = {
    new GoalRepository[F] {
      override def readGoal(config: AppConfig): F[Either[AppError, Option[UUID]]] =
        goalRef.get.map(_.asRight[AppError])

      override def persistGoal(goal: UUID): F[Either[AppError, Unit]] =
        goalRef.set(Some(goal)).map(_.asRight[AppError])
    }
  }
  def dryRun[F[_]: Sync](goalRef: Ref[F, Option[UUID]]): GoalRepository[F] = new GoalRepository[F] {
    override def readGoal(config: AppConfig): F[Either[AppError, Option[UUID]]] = GoalRepository.inMemoryGoalRepository[F](goalRef).readGoal(config)
    override def persistGoal(goal: UUID): F[Either[AppError, Unit]] = ().asRight[AppError].pure[F] //do nothing
  }
}