package com.adewusi.roundup.services

import cats.Monad
import cats.data.EitherT
import com.adewusi.roundup.clients.SavingsGoalClient
import com.adewusi.roundup.model.AppError
import com.adewusi.roundup.repository.GoalRepository

import java.util.UUID

trait GoalService[F[_]] {
  def getOrCreateGoal(accountUid: UUID): F[Either[AppError, UUID]]
}

object GoalService {
  def impl[F[_]: Monad](implicit goalRepository: GoalRepository[F], savingsGoalClient: SavingsGoalClient[F]): GoalService[F] = new GoalService[F] {
    def getOrCreateGoal(accountUid: UUID): F[Either[AppError, UUID]] = {
      val result = for {
        maybeGoal <- EitherT(goalRepository.readGoal)
        goalId <- maybeGoal match {
          case Some(goal) => EitherT(savingsGoalClient.getGoal(goal = goal, accountUid = accountUid)).map(_.savingsGoalUid)
          case None       => EitherT(savingsGoalClient.createGoal(accountUid))
        }
        _ <- EitherT(goalRepository.persistGoal(goalId))
      } yield goalId

      result.value
    }
  }

  /**
    * Dry run implementation that returns the initialGoalId from config if present,
    * otherwise a provided UUID.
    */
  def dryRun[F[_]: Monad](initialGoalUuid: Option[UUID]): GoalService[F] = new GoalService[F] {
    override def getOrCreateGoal(accountUid: UUID): F[Either[AppError, UUID]] = {
      val goalId = initialGoalUuid.getOrElse(UUID.fromString("4FBA270E-9E40-4BAC-AB34-997B91749FA0"))
      Monad[F].pure(Right(goalId))
    }
  }
}
