package com.adewusi.roundup.repository

import cats.effect.{Ref, Sync}
import cats.syntax.all._
import com.adewusi.roundup.model.{AddMoneyResponse, AppError, TransferRecord}

import java.time.LocalDate
import java.util.UUID

trait TransferRepository[F[_]] {
  def isEligibleForTransfer(goal: UUID, startDate: LocalDate, amountMinorUnits: Long): F[Either[AppError, Boolean]]
  def recordTransfer(goal: UUID, startDate: LocalDate, amountMinorUnits: Long, transfer: AddMoneyResponse): F[Either[AppError, Unit]]
}

object TransferRepository {
  // Create a thread-safe mutable reference containing a Set of transfer records
  def inMemoryTransferRepository[F[_]: Sync](ref: Ref[F, Set[TransferRecord]]): TransferRepository[F] = {
    new TransferRepository[F] {

      /**
        * Checks if ANY transfer exists which falls within the 7-day period starting from the given startDate.
        */
      override def isEligibleForTransfer(goal: UUID, startDate: LocalDate, amountMinorUnits: Long): F[Either[AppError, Boolean]] = {
        ref.get.map { transfers =>
          val exists = transfers.exists { t =>
            val windowStart = t.startDate
            val windowEnd = t.startDate.plusDays(6)
            t.goal == goal && !startDate.isBefore(windowStart) && !startDate.isAfter(windowEnd)
          }
          (!exists).asRight[AppError]
        }
      }

      /**
        * Records a successful transfer to prevent future duplicates. Called after Starling API confirms the transfer succeeded.
        */
      override def recordTransfer(goal: UUID, startDate: LocalDate, amountMinorUnits: Long, transfer: AddMoneyResponse): F[Either[AppError, Unit]] = {
        // Create a record of this completed transfer
        val record = TransferRecord(
          goal = goal,
          startDate = startDate,
          amountMinorUnits = amountMinorUnits,
          transferId = transfer.transferUid
        )

        // Add the record to our in-memory set (thread-safe update)
        ref.update(_ + record).map(_.asRight[AppError])
      }
    }
  }
  def dryRun[F[_]: Sync](ref: Ref[F, Set[TransferRecord]]): TransferRepository[F] = ???
}
