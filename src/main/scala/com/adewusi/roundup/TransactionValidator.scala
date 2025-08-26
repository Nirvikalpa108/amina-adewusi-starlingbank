package com.adewusi.roundup

import com.adewusi.roundup.model.{AppError, NoTransactions, TransactionFeedItem}

import java.util.UUID

trait TransactionValidator {
  def validateTransactions(
      transactions: List[TransactionFeedItem]
  ): Either[AppError, List[TransactionFeedItem]]
  def validateRoundupAmount(
      transactions: List[TransactionFeedItem]
  ): Either[AppError, Long]
}

object TransactionValidator {
  def impl(mainCategoryUid: UUID): TransactionValidator =
    new TransactionValidator {

      def validateTransactions(
          transactions: List[TransactionFeedItem]
      ): Either[AppError, List[TransactionFeedItem]] = {
        val validTransactions = transactions.filter(isEligible(_, mainCategoryUid))

        if (validTransactions.isEmpty) { Left(NoTransactions) }
        else { Right(validTransactions) }
      }

      def validateRoundupAmount(
          transactions: List[TransactionFeedItem]
      ): Either[AppError, Long] = {
        validateTransactions(transactions) match {
          case Left(error) => Left(error)
          case Right(validTransactions) =>
            if (validTransactions.isEmpty) {
              Right(0L)
            } else {
              val totalRoundup = validTransactions
                .map(calculateRoundupForTransaction)
                .sum
              Right(totalRoundup)
            }
        }
      }

      def isEligible(tx: TransactionFeedItem, mainCategoryUid: UUID): Boolean =
        tx.direction == "OUT" &&
          tx.amount.currency == "GBP" &&
          tx.status == "SETTLED" &&
          tx.settlementTime.isDefined &&
          tx.categoryUid == mainCategoryUid &&
          tx.roundUp.isEmpty &&
          tx.totalFeeAmount.isEmpty &&
          tx.status != "ACCOUNT_CHECK" &&
          !Set("BANK_CHARGE", "INTEREST_PAYMENT").contains(tx.spendingCategory) &&
          !isInternalTransfer(tx)

      private def isInternalTransfer(tx: TransactionFeedItem): Boolean =
        tx.counterPartyType match {
          case "PAYEE" if tx.counterPartyName.exists(_.toLowerCase.contains("starling")) => true
          case _ => false
        }

      private def calculateRoundupForTransaction(transaction: TransactionFeedItem): Long = {
        // The transaction `amount.minorUnits` is in pence (if GBP).
        // e.g. £4.35 = 435, £5.00 = 500

        val amountInPence = transaction.amount.minorUnits
        val poundsInPence = 100L // 100 pence in a pound

        // If the value is already a whole pound (no pennies left over),
        // then no round-up is needed.
        if (amountInPence % poundsInPence == 0) {
          0L
        } else {
          // Otherwise, figure out the "change" to get to the next full pound.
          //
          // Example:
          //   amountInPence = 435 (£4.35)
          //   remainder     = 435 % 100 = 35
          //   roundup       = 100 - 35 = 65p
          //
          // That means we would save 65p to *round this spend up* to £5.
          val remainder = amountInPence % poundsInPence
          poundsInPence - remainder
        }
      }
    }
}
