package com.adewusi.roundup

import com.adewusi.roundup.model._

import java.util.UUID

/*
1. Goal file check:
  Look at the file.
  If it contains a UUID → go to step 2.
  If it’s empty → go to step 3.
2. Validate goal UUID:
  Call Starling’s Get Savings Goals API.
  If the UUID in the file matches one of the returned goals → accept it and proceed.
  If not → error out (invalid goal, tell the user to delete the file or reset).
3. Create new goal if missing:
  Call Create Savings Goal API.
  Save the returned UUID into the goal file.
  Continue execution using that UUID.
 */
trait SavingsGoalClient[F[_]] {
  def fetchOrCreateSavingsGoal(
      config: AppConfig,
      accountUid: UUID
  ): F[Either[AppError, SavingsGoal]]
  def transferToSavingsGoal(
      config: AppConfig,
      goal: SavingsGoal,
      amountMinorUnits: Long
  ): F[Either[AppError, AddMoneyResponse]]
}

object SavingsGoalClient {

  def impl[F[_]]: SavingsGoalClient[F] = new SavingsGoalClient[F] {
    override def fetchOrCreateSavingsGoal(config: AppConfig, accountUid: UUID): F[Either[AppError, SavingsGoal]] = ???

    override def transferToSavingsGoal(config: AppConfig, goal: SavingsGoal, amountMinorUnits: Long): F[Either[AppError, AddMoneyResponse]] = ???
  }

  //private def readGoalFromFile[F[_]]: F[Either[AppError, UUID]] = ???
  //private def writeGoalToFile[F[_]](id: UUID): F[Unit] = ???

  //def getSavingsGoals(accountUid: String): F[SavingsGoalsResponse]
  //def createSavingsGoal(accountUid: String, request: CreateSavingsGoalRequest): F[CreateSavingsGoalResponse]
}
