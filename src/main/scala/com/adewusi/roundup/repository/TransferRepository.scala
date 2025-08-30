package com.adewusi.roundup.repository

import cats.effect.{Ref, Sync}
import cats.syntax.all._
import com.adewusi.roundup.model.{AddMoneyResponse, AppError, TransferRecord}

import java.time.LocalDate
import java.util.UUID

/** Repository for managing transfer records to savings goals.
  *
  * @tparam F
  *   effect type, typically Cats Effect type like IO
  */
trait TransferRepository[F[_]] {

  /** Checks if a transfer is eligible for the given goal and date. Eligibility
    * means no existing transfer overlaps the 7-day window starting at the given
    * date.
    *
    * @param goal
    *   the UUID of the savings goal
    * @param startDate
    *   the candidate transfer start date
    * @param amountMinorUnits
    *   the amount to transfer in minor currency units (currently unused in
    *   eligibility logic)
    * @return
    *   an effectful computation yielding either an AppError or a Boolean
    *   indicating eligibility
    */
  def isEligibleForTransfer(
      goal: UUID,
      startDate: LocalDate,
      amountMinorUnits: Long
  ): F[Either[AppError, Boolean]]

  /** Records a successful transfer to prevent duplicate transfers within the
    * same window.
    *
    * @param goal
    *   the UUID of the savings goal
    * @param startDate
    *   the start date of the transfer window
    * @param amountMinorUnits
    *   the amount transferred in minor currency units
    * @param transfer
    *   the response from the transfer API containing transfer details
    * @return
    *   an effectful computation yielding either an AppError or Unit on success
    */
  def recordTransfer(
      goal: UUID,
      startDate: LocalDate,
      amountMinorUnits: Long,
      transfer: AddMoneyResponse
  ): F[Either[AppError, Unit]]
}

object TransferRepository {

  /** Creates an in-memory implementation of TransferRepository using a
    * thread-safe Ref.
    *
    * @param ref
    *   a Cats Effect Ref holding a Set of TransferRecords
    * @tparam F
    *   effect type with Sync capability
    * @return
    *   a TransferRepository backed by in-memory state
    */
  def inMemoryTransferRepository[F[_]: Sync](
      ref: Ref[F, Set[TransferRecord]]
  ): TransferRepository[F] = new TransferRepository[F] {

    override def isEligibleForTransfer(
        goal: UUID,
        candidateDate: LocalDate,
        amountMinorUnits: Long
    ): F[Either[AppError, Boolean]] = {
      ref.get.map { transfers =>
        val exists = transfers.exists { t =>
          val windowStart = t.startDate
          val windowEnd = t.startDate.plusDays(6)
          t.goal == goal && !candidateDate.isBefore(
            windowStart
          ) && !candidateDate.isAfter(windowEnd)
        }
        (!exists).asRight[AppError]
      }
    }

    /**
      * Records a successful transfer by adding a new TransferRecord to the in-memory store.
      *
      * This method atomically updates the internal Ref containing the set of transfer records,
      * adding the provided transfer record to prevent duplicate transfers within the same window.
      * It returns a successful result wrapped in the effect type `F`.
      *
      * @param goal             the UUID of the savings goal associated with the transfer
      * @param startDate        the start date of the transfer window
      * @param amountMinorUnits the amount transferred in minor currency units (e.g., pence)
      * @param transfer         the AddMoneyResponse containing details of the completed transfer, including transfer UID
      * @return an effectful computation yielding either an AppError or Unit on success
      */
    override def recordTransfer(
        goal: UUID,
        startDate: LocalDate,
        amountMinorUnits: Long,
        transfer: AddMoneyResponse
    ): F[Either[AppError, Unit]] = {
      val record =
        TransferRecord(goal, startDate, amountMinorUnits, transfer.transferUid)
      ref.update(_ + record) *> Sync[F].pure(().asRight[AppError])
    }
  }

  /** Creates a dry-run implementation of TransferRepository that no-ops on
    * recordTransfer.
    *
    * @param ref
    *   a Cats Effect Ref holding a Set of TransferRecords
    * @tparam F
    *   effect type with Sync capability
    * @return
    *   a TransferRepository that simulates behavior without side effects
    */
  def dryRun[F[_]: Sync](
      ref: Ref[F, Set[TransferRecord]]
  ): TransferRepository[F] = new TransferRepository[F] {
    private val transferRepo = inMemoryTransferRepository(ref)

    override def isEligibleForTransfer(
        goal: UUID,
        startDate: LocalDate,
        amountMinorUnits: Long
    ): F[Either[AppError, Boolean]] =
      transferRepo.isEligibleForTransfer(goal, startDate, amountMinorUnits)

    override def recordTransfer(
        goal: UUID,
        startDate: LocalDate,
        amountMinorUnits: Long,
        transfer: AddMoneyResponse
    ): F[Either[AppError, Unit]] =
      Sync[F].pure(().asRight[AppError]) // Returns a successful effect without performing any action (no-op).
  }
}
