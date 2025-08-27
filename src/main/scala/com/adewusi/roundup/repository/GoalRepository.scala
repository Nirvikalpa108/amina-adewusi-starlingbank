package com.adewusi.roundup.repository

import com.adewusi.roundup.model.{AppConfig, AppError}

import java.util.UUID

trait GoalRepository[F[_]] {
  def readGoal(config: AppConfig): F[Either[AppError, Option[UUID]]]
  def persistGoal(goal: UUID): F[Either[AppError, Unit]]
}
