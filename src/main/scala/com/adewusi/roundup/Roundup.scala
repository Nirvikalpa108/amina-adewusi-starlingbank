package com.adewusi.roundup

import cats.data.EitherT
import cats.effect.Sync
import com.adewusi.roundup.model._
import java.time.LocalDate
import java.util.UUID

object Roundup {

  def processRoundups[F[_]: Sync](startDate: LocalDate, config: AppConfig, savingsGoalId: Option[UUID]): EitherT[F, AppError, Unit] = {
    for {
      accounts <- fetchAccounts[F](config)
      account <- EitherT.fromEither[F](getCorrectAccount(accounts))
      transactions <- fetchTransactions[F](config, account, startDate)
      validatedTransactions  <- EitherT.fromEither[F](validateTransactions(transactions))
      roundup   <- EitherT.fromEither[F](validateRoundupAmount(validatedTransactions))
      goal <- fetchOrCreateSavingsGoal[F](config, savingsGoalId)
      // NOTE: In production, idempotency + recording should be atomic to avoid race conditions.
      // This test assumes single-instance execution.
      needsProcessing <- checkIdempotency[F](goal, startDate, roundup)
      _ <- EitherT.cond[F](needsProcessing, (), AlreadyTransferred(s"Roundup for $startDate already processed"))
      transfer <- transferToSavingsGoal[F](config, goal, roundup)
      _ <- recordTransfer[F](goal, startDate, roundup, transfer)
    } yield ()
  }

  def fetchAccounts[F[_] : Sync](config: AppConfig): EitherT[F, AppError, List[Account]] = ???
  def getCorrectAccount(accounts: List[Account]): Either[AppError, Account] = ???
  def fetchTransactions[F[_]: Sync](config: AppConfig, account: Account, startDate: LocalDate): EitherT[F, AppError, List[TransactionFeedItem]] = ???
  def validateTransactions(transactions: List[TransactionFeedItem]): Either[AppError, List[TransactionFeedItem]] = ???
  def validateRoundupAmount(transactions: List[TransactionFeedItem]): Either[AppError, Long] = ???
  def fetchOrCreateSavingsGoal[F[_]: Sync](config: AppConfig, maybeGoal: Option[UUID]): EitherT[F, AppError, SavingsGoal] = ???
  def checkIdempotency[F[_]: Sync](goal: SavingsGoal, startDate: LocalDate, amountMinorUnits: Long): EitherT[F, AppError, Boolean] = ???
  def transferToSavingsGoal[F[_]: Sync](config: AppConfig, goal: SavingsGoal, amountMinorUnits: Long): EitherT[F, AppError, AddMoneyResponse] = ???
  def recordTransfer[F[_]: Sync](goal: SavingsGoal, startDate: LocalDate, amountMinorUnits: Long, transfer: AddMoneyResponse): EitherT[F, AppError, Unit] = ???
}

trait AccountClient[F[_]] {
  def fetchAccounts(config: AppConfig): F[Either[AppError, List[Account]]]
}

trait TransactionClient[F[_]] {
  def fetchTransactions(config: AppConfig, account: Account, startDate: LocalDate): F[Either[AppError, List[TransactionFeedItem]]]
}

trait SavingsGoalClient[F[_]] {
  def fetchOrCreateSavingsGoal(config: AppConfig, maybeGoal: Option[UUID]): F[Either[AppError, SavingsGoal]]
  def transferToSavingsGoal(config: AppConfig, goal: SavingsGoal, amountMinorUnits: Long): F[Either[AppError, AddMoneyResponse]]
}

trait IdempotencyClient[F[_]] {
  def checkIdempotency(goal: SavingsGoal, startDate: LocalDate, amountMinorUnits: Long): F[Either[AppError, Boolean]]
  def recordTransfer(goal: SavingsGoal, startDate: LocalDate, amountMinorUnits: Long, transfer: AddMoneyResponse): F[Either[AppError, Unit]]
}

trait AccountSelector {
  def getCorrectAccount(accounts: List[Account]): Either[AppError, Account]
}

trait TransactionValidator {
  def validateTransactions(transactions: List[TransactionFeedItem]): Either[AppError, List[TransactionFeedItem]]
  def validateRoundupAmount(transactions: List[TransactionFeedItem]): Either[AppError, Long]
}
