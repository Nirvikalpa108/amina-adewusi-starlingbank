package com.adewusi.roundup

import cats.data.EitherT
import cats.effect.Sync
import com.adewusi.roundup.model._

import java.time.LocalDate
import java.util.UUID

object Roundup {

  //TODO move to model
  sealed trait AppError
  sealed trait DomainError extends AppError
  sealed trait InfraError extends AppError

  case object NoTransactions extends DomainError
  case object ZeroRoundupAmount extends DomainError
  final case class AlreadyTransferred(reason: String) extends DomainError

  final case class NotFoundError(reason: String) extends InfraError
  final case class TransferError(reason: String) extends InfraError

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
