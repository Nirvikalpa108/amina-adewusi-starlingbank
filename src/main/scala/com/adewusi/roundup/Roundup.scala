package com.adewusi.roundup

import cats.data.EitherT
import cats.effect.Sync
import com.adewusi.roundup.model._

import java.time.LocalDate
import java.util.UUID

object Roundup {

  //TODO move to model
  sealed trait AppError
  case class ParseDateError(reason: String) extends AppError
  case class NotFoundError(reason: String) extends AppError
  case class TransferError(reason: String) extends AppError
  case object NoTransactions extends AppError
  case class AlreadyTransferred(reason: String) extends AppError
  case class GenericError(reason: String) extends AppError

  def processRoundups[F[_]: Sync](startDate: LocalDate, config: AppConfig, savingsGoalId: Option[UUID]): EitherT[F, AppError, Unit] = {
    for {
      accounts <- fetchAccounts[F](config)
      account <- EitherT.fromEither[F](getCorrectAccount(accounts))
      txs <- fetchTransactions[F](config, account, startDate)
      filtered = filterTransactions(txs)
      roundup = computeRoundupTotal(filtered)
      goal <- fetchOrCreateSavingsGoal[F](config, savingsGoalId)
      already <- checkIdempotency[F](goal, startDate, roundup)
      _ <- if (already) EitherT.leftT[F, Unit](AlreadyTransferred("Transfer already recorded"))
      else EitherT.rightT[F, AppError](())
      transfer <- transferToSavingsGoal[F](config, goal, roundup)
      _ <- appendTransferRecord[F](goal, startDate, roundup, transfer)
    } yield ()
  }

  def fetchAccounts[F[_] : Sync](config: AppConfig): EitherT[F, AppError, List[Account]] = ???
  def getCorrectAccount(accounts: List[Account]): Either[AppError, Account] = ???
  def fetchTransactions[F[_]: Sync](config: AppConfig, account: Account, startDate: LocalDate): EitherT[F, AppError, List[TransactionFeedItem]] = ???
  def filterTransactions(transactions: List[TransactionFeedItem]): List[TransactionFeedItem] = ???
  def computeRoundupTotal(transactions: List[TransactionFeedItem]): Long = ???
  def fetchOrCreateSavingsGoal[F[_]: Sync](config: AppConfig, maybeGoal: Option[UUID]): EitherT[F, AppError, SavingsGoal] = ???
  def checkIdempotency[F[_]: Sync](goal: SavingsGoal, startDate: LocalDate, amountMinorUnits: Long): EitherT[F, AppError, Boolean] = ???
  def transferToSavingsGoal[F[_]: Sync](config: AppConfig, goal: SavingsGoal, amountMinorUnits: Long): EitherT[F, AppError, AddMoneyResponse] = ???
  def appendTransferRecord[F[_]: Sync](goal: SavingsGoal, startDate: LocalDate, amountMinorUnits: Long, transfer: AddMoneyResponse): EitherT[F, AppError, Unit] = ???
}
