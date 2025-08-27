package com.adewusi.roundup

import cats.Monad
import cats.data.EitherT
import cats.effect.Sync
import com.adewusi.roundup.model._

import java.time.LocalDate
import java.util.UUID

object Roundup {

  def processRoundups[F[_]: Sync](
      startDate: LocalDate,
      config: AppConfig
  )(implicit
    accountClient: AccountClient[F],
    transactionClient: TransactionClient[F],
    savingsGoalClient: SavingsGoalClient[F],
    goalRepository: GoalRepository[F],
    goalService: GoalService[F],
    transferLedger: TransferRepository[F],
    accountSelector: AccountSelector,
    transactionValidator: TransactionValidator
  ): EitherT[F, AppError, Unit] = {
    for {
      accounts <- EitherT(accountClient.fetchAccounts)
      account <- EitherT.fromEither(accountSelector.getCorrectAccount(accounts))
      transactions <- EitherT(transactionClient.fetchTransactions(account, startDate))
      validatedTransactions <- EitherT.fromEither(transactionValidator.validateTransactions(transactions))
      roundup <- EitherT.fromEither(transactionValidator.validateRoundupAmount(validatedTransactions))
      goalUuid <- EitherT(goalService.getOrCreateGoal(config, account.accountUid))
      _ <- EitherT(goalRepository.persistGoal(goalUuid))
      needsProcessing <- EitherT(transferLedger.isEligibleForTransfer(goalUuid, startDate, roundup))
      _ <- EitherT.cond[F](needsProcessing, (), AlreadyTransferred(s"Roundup for $startDate already processed"))
      transfer <- EitherT(savingsGoalClient.transferToGoal(account.accountUid, goalUuid, roundup))
      _ <- EitherT(transferLedger.recordTransfer(goalUuid, startDate, roundup, transfer))
    } yield ()
  }
}

trait GoalRepository[F[_]] {
  def readGoal(config: AppConfig): F[Either[AppError, Option[UUID]]]
  def persistGoal(goal: UUID): F[Either[AppError, Unit]]
}

trait TransferRepository[F[_]] {
  def isEligibleForTransfer(goal: UUID, startDate: LocalDate, amountMinorUnits: Long): F[Either[AppError, Boolean]]
  def recordTransfer(goal: UUID, startDate: LocalDate, amountMinorUnits: Long, transfer: AddMoneyResponse): F[Either[AppError, Unit]]
}

trait SavingsGoalClient[F[_]] {
  def getGoal(goal: UUID, accountUid: UUID): F[Either[AppError, SavingsGoal]]
  def createGoal(accountUid: UUID): F[Either[AppError, UUID]]
  def transferToGoal(accountUid: UUID, goal: UUID, amountMinorUnits: Long): F[Either[AppError, AddMoneyResponse]]
}

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
