package com.adewusi.roundup.clients

import com.adewusi.roundup.model.{AddMoneyResponse, AppError, SavingsGoal}

import java.util.UUID

trait SavingsGoalClient[F[_]] {
  def getGoal(goal: UUID, accountUid: UUID): F[Either[AppError, SavingsGoal]]
  def createGoal(accountUid: UUID): F[Either[AppError, UUID]]
  def transferToGoal(accountUid: UUID, goal: UUID, amountMinorUnits: Long): F[Either[AppError, AddMoneyResponse]]
}