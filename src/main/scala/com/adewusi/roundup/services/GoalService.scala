package com.adewusi.roundup.services

import cats.Monad
import cats.data.EitherT
import com.adewusi.roundup.clients.SavingsGoalClient
import com.adewusi.roundup.model.{AppConfig, AppError}
import com.adewusi.roundup.repository.GoalRepository

import java.util.UUID

trait GoalService[F[_]] {
  def getOrCreateGoal(config: AppConfig, accountUid: UUID): F[Either[AppError, UUID]]
}

object GoalService {
  def impl[F[_]: Monad](goalRepository: GoalRepository[F], savingsGoalClient: SavingsGoalClient[F]): GoalService[F] = new GoalService[F] {
    def getOrCreateGoal(config: AppConfig, accountUid: UUID): F[Either[AppError, UUID]] = {
      val result = for {
        maybeGoal <- EitherT(goalRepository.readGoal(config))
        goalId <- maybeGoal match {
          case Some(goal) => EitherT(savingsGoalClient.getGoal(goal, accountUid)).map(_.savingsGoalUid)
          case None       => EitherT(savingsGoalClient.createGoal(accountUid))
        }
        _ <- EitherT(goalRepository.persistGoal(goalId))
      } yield goalId

      result.value
    }
  }
}
