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
  def inMemoryTransferRepository[F[_]: Sync]: F[TransferRepository[F]] = {
    // Create a thread-safe mutable reference containing a Set of transfer records
    // Starts empty - no transfers have been made yet
    Ref.of[F, Set[TransferRecord]](Set.empty).map { transfersRef =>

      new TransferRepository[F] {

        /**
          * Checks if ANY transfer exists whose startDate falls within the 7-day period starting from the given startDate.
          */
        override def isEligibleForTransfer(goal: UUID, startDate: LocalDate, amountMinorUnits: Long): F[Either[AppError, Boolean]] = {
          transfersRef.get.map { transfers =>
            val exists = transfers.exists { t =>
              t.goal == goal &&
                !startDate.isBefore(t.startDate) &&        // query >= recorded start
                !startDate.isAfter(t.startDate.plusDays(6)) // query <= recorded+6
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
          transfersRef.update(_ + record).map(_.asRight[AppError])
        }
      }
    }
  }
}
