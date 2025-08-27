package com.adewusi.roundup.repository

import com.adewusi.roundup.model.{AddMoneyResponse, AppError}

import java.time.LocalDate
import java.util.UUID

trait TransferRepository[F[_]] {
  def isEligibleForTransfer(goal: UUID, startDate: LocalDate, amountMinorUnits: Long): F[Either[AppError, Boolean]]
  def recordTransfer(goal: UUID, startDate: LocalDate, amountMinorUnits: Long, transfer: AddMoneyResponse): F[Either[AppError, Unit]]
}
