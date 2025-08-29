package com.adewusi.roundup.clients

import cats.implicits._
import cats.effect.Concurrent
import com.adewusi.roundup.model._
import com.adewusi.roundup.starlingapis.StarlingSavingsGoalsApi

import java.util.UUID

trait SavingsGoalClient[F[_]] {
  def getGoal(goal: UUID, accountUid: UUID): F[Either[AppError, SavingsGoal]]
  def createGoal(accountUid: UUID): F[Either[AppError, UUID]]
  def transferToGoal(accountUid: UUID, goal: UUID, amountMinorUnits: Long): F[Either[AppError, AddMoneyResponse]]
}

object SavingsGoalClient {
  def impl[F[_] : Concurrent](implicit starlingSavingsGoalsApi: StarlingSavingsGoalsApi[F]): SavingsGoalClient[F] = new SavingsGoalClient[F] {
    override def getGoal(goal: UUID, accountUid: UUID): F[Either[AppError, SavingsGoal]] = {
      starlingSavingsGoalsApi.getSavingsGoals(accountUid.toString).attempt.map {
        case Right(savingsGoalsResponse) =>
          savingsGoalsResponse.savingsGoalList
            .find(_.savingsGoalUid == goal)
            .toRight(InvalidStoredSavingsGoalUuid(s"Savings goal with ID $goal not found"))
        case Left(throwable) =>
          Left(GenericError(s"Failed to retrieve savings goals: ${throwable.getMessage}"))
      }
    }
    override def createGoal(accountUid: UUID): F[Either[AppError, UUID]] = {
      val createRequest = CreateSavingsGoalRequest(name = "Round-up Savings", currency = "GBP")
      starlingSavingsGoalsApi.createSavingsGoal(accountUid.toString, createRequest).attempt.map {
        case Right(createResponse) if createResponse.success =>
          Right(createResponse.savingsGoalUid)
        case Right(_) =>
          Left(GenericError("Failed to create savings goal - API returned false"))
        case Left(throwable) =>
          Left(GenericError(s"Error when calling the savings goal API: ${throwable.getMessage}"))
      }
    }
    override def transferToGoal(accountUid: UUID, goal: UUID, amountMinorUnits: Long): F[Either[AppError, AddMoneyResponse]] = {
      val transferUid = UUID.randomUUID()
      val maybeRef: Option[Reference] = Reference.fromString("Round-up transfer").toOption
      val addMoneyRequest = AddMoneyRequest(
        amount = CurrencyAndAmount(
          currency = "GBP",
          minorUnits = amountMinorUnits
        ),
        reference = maybeRef
      )

      starlingSavingsGoalsApi.addMoney(
        accountUid.toString,
        goal.toString,
        transferUid.toString,
        addMoneyRequest
      ).attempt.map {
        case Right(addMoneyResponse) if addMoneyResponse.success =>
          Right(addMoneyResponse)
        case Right(_) =>
          Left(TransferError("Failed to transfer money to savings goal - API returned success=false"))
        case Left(throwable) =>
          Left(TransferError(s"Failed to transfer money to savings goal: ${throwable.getMessage}"))
      }
    }
  }

  def dryRun[F[_] : Concurrent]: SavingsGoalClient[F] = new SavingsGoalClient[F] {
    override def getGoal(goal: UUID, accountUid: UUID): F[Either[AppError, SavingsGoal]] =
      Concurrent[F].pure(
        Right(
          SavingsGoal(
            savingsGoalUid = goal,
            name = "Dry Run Goal",
            target = None,
            totalSaved = CurrencyAndAmount("GBP", 0),
            savedPercentage = Some(0),
            state = "ACTIVE"
          )
        )
      )

    override def createGoal(accountUid: UUID): F[Either[AppError, UUID]] = {
      val dryRunGoalId = UUID.nameUUIDFromBytes(s"dry-run-goal-${accountUid}".getBytes)
      Concurrent[F].pure(Right(dryRunGoalId))
    }

    override def transferToGoal(accountUid: UUID, goal: UUID, amountMinorUnits: Long): F[Either[AppError, AddMoneyResponse]] =
      Concurrent[F].pure(
        Right(AddMoneyResponse(success = true, transferUid = UUID.randomUUID()))
      )
  }
}