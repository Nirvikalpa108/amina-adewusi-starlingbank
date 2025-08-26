package com.adewusi.roundup

import com.adewusi.roundup.model._

import java.util.UUID

/*
- If UUID provided → must exist in Starling’s API. If not, error.
- Else if local file contains UUID → confirm with Starling API.
    - if exists in Starling → ✅ use
    - if not → go to (3).
- Else (or stale file) → create a new goal and persist to file.
 */
//TODO look at Starling savings goal API to see if signature needs to change
//Not sure if we need goal cache. Spend a bit more time thinking about this.
trait SavingsGoalClient[F[_]] {
  // Read from a file (optional cache of UUID).
  // Possibly write to that file after creating new goal.
  def fetchOrCreateSavingsGoal(
      config: AppConfig,
      accountUid: String,
      maybeGoal: Option[UUID]
  ): F[Either[AppError, SavingsGoal]]
  def transferToSavingsGoal(
      config: AppConfig,
      goal: SavingsGoal,
      amountMinorUnits: Long
  ): F[Either[AppError, AddMoneyResponse]]
}
