package com.adewusi.roundup.domain

import com.adewusi.roundup.model.{AppError, NoTransactions, TransactionFeedItem}

import java.util.UUID

trait TransactionValidator {
  def validateTransactions(
      transactions: List[TransactionFeedItem],
      mainCategoryUid: UUID
  ): Either[AppError, List[TransactionFeedItem]]
  def validateRoundupAmount(
      transactions: List[TransactionFeedItem],
      mainCategoryUid: UUID
  ): Either[AppError, Long]
}

object TransactionValidator {
  def impl: TransactionValidator =
    new TransactionValidator {

      def validateTransactions(
          transactions: List[TransactionFeedItem],
          mainCategoryUid: UUID
      ): Either[AppError, List[TransactionFeedItem]] = {
        val validTransactions =
          transactions.filter(isEligible(_, mainCategoryUid))

        if (validTransactions.isEmpty) { Left(NoTransactions) }
        else { Right(validTransactions) }
      }

      def validateRoundupAmount(
          transactions: List[TransactionFeedItem],
          mainCategoryUid: UUID
      ): Either[AppError, Long] = {
        validateTransactions(transactions, mainCategoryUid) match {
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

      /**
        * Determines whether a given transaction is eligible for round-up.
        *
        * A transaction is considered eligible if it meets all the following criteria:
        *   - The transaction direction is "OUT" (i.e., an outgoing payment).
        *   - The transaction currency is GBP.
        *   - The transaction status is "SETTLED".
        *   - The transaction has a defined settlement time.
        *   - The transaction belongs to the specified main category (matching `mainCategoryUid`).
        *   - The transaction has not already been rounded up (`roundUp` is empty).
        *   - The transaction does not have any associated fees (`totalFeeAmount` is empty).
        *   - The transaction status is not "ACCOUNT_CHECK".
        *   - The spending category is not one of the excluded categories such as "BANK_CHARGE" or "INTEREST_PAYMENT".
        *   - The transaction is not an internal transfer within Starling accounts.
        *
        * @param tx              The transaction to evaluate.
        * @param mainCategoryUid The UUID of the main category to filter transactions by.
        * @return true if the transaction is eligible for round-up; false otherwise.
        */
      def isEligible(tx: TransactionFeedItem, mainCategoryUid: UUID): Boolean = {
        val excludedStatuses = Set("ACCOUNT_CHECK")
        val excludedCategories = Set("BANK_CHARGE", "INTEREST_PAYMENT")

        tx.direction == "OUT" &&
          tx.amount.currency == "GBP" &&
          tx.status == "SETTLED" &&
          tx.settlementTime.isDefined &&
          tx.categoryUid == mainCategoryUid &&
          tx.roundUp.isEmpty &&
          tx.totalFeeAmount.isEmpty &&
          !excludedStatuses.contains(tx.status) &&
          !excludedCategories.contains(tx.spendingCategory) &&
          !isInternalTransfer(tx)
      }

      private def isInternalTransfer(tx: TransactionFeedItem): Boolean =
        tx.counterPartyType match {
          case "PAYEE"
              if tx.counterPartyName.exists(
                _.toLowerCase.contains("starling")
              ) =>
            true
          case _ => false
        }

      /**
        * Calculates the round-up amount in pence needed to round a transaction's amount
        * up to the nearest whole pound.
        *
        * For example, if the transaction amount is £4.35 (435 pence), the round-up amount
        * will be 65 pence to reach £5.00.
        *
        * Assumes the transaction currency is GBP and that `amount.minorUnits` represents
        * the amount in pence.
        *
        * @param transaction The transaction for which to calculate the round-up amount.
        * @return The round-up amount in pence (Long). Returns 0 if the amount is already a whole pound.
        */
      private def calculateRoundupForTransaction(transaction: TransactionFeedItem): Long = {
        val amountInPence = transaction.amount.minorUnits
        val poundsInPence = 100L

        if (amountInPence % poundsInPence == 0) 0L
        else poundsInPence - (amountInPence % poundsInPence)
      }
    }
}
