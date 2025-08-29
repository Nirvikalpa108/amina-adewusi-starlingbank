package com.adewusi.roundup.domain

import com.adewusi.roundup.model.{AssociatedFeedRoundUp, CurrencyAndAmount, NoTransactions, TransactionFeedItem}
import munit.CatsEffectSuite

import java.time.ZonedDateTime
import java.util.UUID

class TransactionValidatorSpec extends CatsEffectSuite {

  private val mainCategoryUid = UUID.randomUUID()
  private val validator = TransactionValidator.impl

  private def createValidTransaction(
      amountMinorUnits: Long,
      direction: String = "OUT",
      currency: String = "GBP",
      status: String = "SETTLED",
      categoryUid: UUID = mainCategoryUid,
      settlementTime: Option[ZonedDateTime] = Some(ZonedDateTime.now()),
      roundUp: Option[AssociatedFeedRoundUp] = None,
      totalFeeAmount: Option[CurrencyAndAmount] = None,
      spendingCategory: String = "GENERAL",
      counterPartyType: String = "MERCHANT",
      counterPartyName: Option[String] = Some("Test Merchant")
  ): TransactionFeedItem = {
    TransactionFeedItem(
      feedItemUid = UUID.randomUUID(),
      categoryUid = categoryUid,
      amount = CurrencyAndAmount(currency, amountMinorUnits),
      sourceAmount = CurrencyAndAmount(currency, amountMinorUnits),
      direction = direction,
      updatedAt = ZonedDateTime.now(),
      transactionTime = ZonedDateTime.now(),
      settlementTime = settlementTime,
      retryAllocationUntilTime = None,
      source = "FASTER_PAYMENTS_IN",
      sourceSubType = None,
      status = status,
      transactingApplicationUserUid = None,
      counterPartyType = counterPartyType,
      counterPartyUid = None,
      counterPartyName = counterPartyName,
      counterPartySubEntityUid = None,
      counterPartySubEntityName = None,
      counterPartySubEntityIdentifier = None,
      counterPartySubEntitySubIdentifier = None,
      exchangeRate = None,
      totalFees = None,
      totalFeeAmount = totalFeeAmount,
      reference = Some("Test transaction"),
      country = "GB",
      spendingCategory = spendingCategory,
      userNote = None,
      roundUp = roundUp,
      hasAttachment = None,
      hasReceipt = None,
      batchPaymentDetails = None
    )
  }

  test(
    "validateTransactions should return valid transactions when all transactions are eligible"
  ) {
    val transactions = List(
      createValidTransaction(435L), // £4.35
      createValidTransaction(250L), // £2.50
      createValidTransaction(100L) // £1.00
    )

    val result = validator.validateTransactions(transactions, mainCategoryUid)

    assertEquals(result.isRight, true)
    result.foreach { validTxs =>
      assertEquals(validTxs.length, 3)
    }
  }

  test("validateTransactions should filter out ineligible transactions") {
    val transactions = List(
      createValidTransaction(435L), // Valid
      createValidTransaction(
        250L,
        direction = "IN"
      ), // Invalid - wrong direction
      createValidTransaction(
        300L,
        currency = "USD"
      ), // Invalid - wrong currency
      createValidTransaction(150L, status = "PENDING") // Invalid - wrong status
    )

    val result = validator.validateTransactions(transactions, mainCategoryUid)

    assertEquals(result.isRight, true)
    result.foreach { validTxs =>
      assertEquals(validTxs.length, 1)
      assertEquals(validTxs.head.amount.minorUnits, 435L)
    }
  }

  test(
    "validateTransactions should return NoTransactions error when no transactions are eligible"
  ) {
    val transactions = List(
      createValidTransaction(
        250L,
        direction = "IN"
      ), // Invalid - wrong direction
      createValidTransaction(
        300L,
        currency = "USD"
      ), // Invalid - wrong currency
      createValidTransaction(150L, status = "PENDING") // Invalid - wrong status
    )

    val result = validator.validateTransactions(transactions, mainCategoryUid)

    assertEquals(result.isLeft, true)
    result.left.foreach { error =>
      assertEquals(error, NoTransactions)
    }
  }

  test(
    "validateTransactions should return NoTransactions error for empty list"
  ) {
    val result = validator.validateTransactions(List.empty, mainCategoryUid)

    assertEquals(result.isLeft, true)
    result.left.foreach { error =>
      assertEquals(error, NoTransactions)
    }
  }

  test(
    "validateTransactions should filter out transactions with wrong category"
  ) {
    val wrongCategoryUid = UUID.randomUUID()
    val transactions = List(
      createValidTransaction(435L), // Valid - correct category
      createValidTransaction(
        250L,
        categoryUid = wrongCategoryUid
      ) // Invalid - wrong category
    )

    val result = validator.validateTransactions(transactions, mainCategoryUid)

    assertEquals(result.isRight, true)
    result.foreach { validTxs =>
      assertEquals(validTxs.length, 1)
      assertEquals(validTxs.head.amount.minorUnits, 435L)
    }
  }

  test(
    "validateTransactions should filter out transactions without settlement time"
  ) {
    val transactions = List(
      createValidTransaction(435L), // Valid - has settlement time
      createValidTransaction(
        250L,
        settlementTime = None
      ) // Invalid - no settlement time
    )

    val result = validator.validateTransactions(transactions, mainCategoryUid)

    assertEquals(result.isRight, true)
    result.foreach { validTxs =>
      assertEquals(validTxs.length, 1)
      assertEquals(validTxs.head.amount.minorUnits, 435L)
    }
  }

  test("validateTransactions should filter out transactions with roundUp") {
    val roundUpData = AssociatedFeedRoundUp(
      goalCategoryUid = UUID.randomUUID(),
      amount = CurrencyAndAmount("GBP", 50L)
    )
    val transactions = List(
      createValidTransaction(435L), // Valid - no roundUp
      createValidTransaction(
        250L,
        roundUp = Some(roundUpData)
      ) // Invalid - has roundUp
    )

    val result = validator.validateTransactions(transactions, mainCategoryUid)

    assertEquals(result.isRight, true)
    result.foreach { validTxs =>
      assertEquals(validTxs.length, 1)
      assertEquals(validTxs.head.amount.minorUnits, 435L)
    }
  }

  test(
    "validateTransactions should filter out transactions with totalFeeAmount"
  ) {
    val feeAmount = CurrencyAndAmount("GBP", 50L)
    val transactions = List(
      createValidTransaction(435L), // Valid - no fee
      createValidTransaction(
        250L,
        totalFeeAmount = Some(feeAmount)
      ) // Invalid - has fee
    )

    val result = validator.validateTransactions(transactions, mainCategoryUid)

    assertEquals(result.isRight, true)
    result.foreach { validTxs =>
      assertEquals(validTxs.length, 1)
      assertEquals(validTxs.head.amount.minorUnits, 435L)
    }
  }

  test("validateTransactions should filter out BANK_CHARGE spending category") {
    val transactions = List(
      createValidTransaction(435L), // Valid - GENERAL category
      createValidTransaction(
        250L,
        spendingCategory = "BANK_CHARGE"
      ) // Invalid - BANK_CHARGE
    )

    val result = validator.validateTransactions(transactions, mainCategoryUid)

    assertEquals(result.isRight, true)
    result.foreach { validTxs =>
      assertEquals(validTxs.length, 1)
      assertEquals(validTxs.head.amount.minorUnits, 435L)
    }
  }

  test(
    "validateTransactions should filter out INTEREST_PAYMENT spending category"
  ) {
    val transactions = List(
      createValidTransaction(435L), // Valid - GENERAL category
      createValidTransaction(
        250L,
        spendingCategory = "INTEREST_PAYMENT"
      ) // Invalid - INTEREST_PAYMENT
    )

    val result = validator.validateTransactions(transactions, mainCategoryUid)

    assertEquals(result.isRight, true)
    result.foreach { validTxs =>
      assertEquals(validTxs.length, 1)
      assertEquals(validTxs.head.amount.minorUnits, 435L)
    }
  }

  test("validateTransactions should filter out internal Starling transfers") {
    val transactions = List(
      createValidTransaction(435L), // Valid - regular merchant
      createValidTransaction(
        250L,
        counterPartyType = "PAYEE",
        counterPartyName = Some("Starling Bank")
      ) // Invalid - internal transfer
    )

    val result = validator.validateTransactions(transactions, mainCategoryUid)

    assertEquals(result.isRight, true)
    result.foreach { validTxs =>
      assertEquals(validTxs.length, 1)
      assertEquals(validTxs.head.amount.minorUnits, 435L)
    }
  }

  test("validateTransactions should filter out ACCOUNT_CHECK status") {
    val transactions = List(
      createValidTransaction(435L), // Valid - SETTLED status
      createValidTransaction(
        250L,
        status = "ACCOUNT_CHECK"
      ) // Invalid - ACCOUNT_CHECK status
    )

    val result = validator.validateTransactions(transactions, mainCategoryUid)

    assertEquals(result.isRight, true)
    result.foreach { validTxs =>
      assertEquals(validTxs.length, 1)
      assertEquals(validTxs.head.amount.minorUnits, 435L)
    }
  }

  test(
    "validateRoundupAmount should calculate correct roundup for valid transactions"
  ) {
    val transactions = List(
      createValidTransaction(435L), // £4.35 -> 65p roundup
      createValidTransaction(250L), // £2.50 -> 50p roundup
      createValidTransaction(199L) // £1.99 -> 1p roundup
    )

    val result = validator.validateRoundupAmount(transactions, mainCategoryUid)

    assertEquals(result.isRight, true)
    result.foreach { roundupAmount =>
      // 65 + 50 + 1 = 116 pence
      assertEquals(roundupAmount, 116L)
    }
  }

  test(
    "validateRoundupAmount should return 0 for transactions that are whole pounds"
  ) {
    val transactions = List(
      createValidTransaction(500L), // £5.00 -> 0p roundup
      createValidTransaction(1000L) // £10.00 -> 0p roundup
    )

    val result = validator.validateRoundupAmount(transactions, mainCategoryUid)

    assertEquals(result.isRight, true)
    result.foreach { roundupAmount =>
      assertEquals(roundupAmount, 0L)
    }
  }

  test(
    "validateRoundupAmount should handle mixed whole pounds and partial amounts"
  ) {
    val transactions = List(
      createValidTransaction(500L), // £5.00 -> 0p roundup
      createValidTransaction(435L), // £4.35 -> 65p roundup
      createValidTransaction(1000L) // £10.00 -> 0p roundup
    )

    val result = validator.validateRoundupAmount(transactions, mainCategoryUid)

    assertEquals(result.isRight, true)
    result.foreach { roundupAmount =>
      assertEquals(roundupAmount, 65L)
    }
  }

  test(
    "validateRoundupAmount should return NoTransactions error when no valid transactions"
  ) {
    val transactions = List(
      createValidTransaction(
        250L,
        direction = "IN"
      ), // Invalid - wrong direction
      createValidTransaction(300L, currency = "USD") // Invalid - wrong currency
    )

    val result = validator.validateTransactions(transactions, mainCategoryUid)

    assertEquals(result.isLeft, true)
    result.left.foreach { error =>
      assertEquals(error, NoTransactions)
    }
  }

  test(
    "validateRoundupAmount should return NoTransactions error for empty transaction list"
  ) {
    val result = validator.validateRoundupAmount(List.empty, mainCategoryUid)

    assertEquals(result.isLeft, true)
    result.left.foreach { error =>
      assertEquals(error, NoTransactions)
    }
  }

  test(
    "calculateRoundupForTransaction should calculate correct roundup amounts"
  ) {
    // Test various amounts and their expected roundups
    val testCases = List(
      (435L, 65L), // £4.35 -> 65p
      (250L, 50L), // £2.50 -> 50p
      (199L, 1L), // £1.99 -> 1p
      (500L, 0L), // £5.00 -> 0p
      (1L, 99L), // £0.01 -> 99p
      (999L, 1L), // £9.99 -> 1p
      (1000L, 0L) // £10.00 -> 0p
    )

    testCases.foreach { case (amount, expectedRoundup) =>
      val transaction = createValidTransaction(amount)
      val transactions = List(transaction)

      val result = validator.validateRoundupAmount(transactions, mainCategoryUid)

      assertEquals(result.isRight, true, s"Failed for amount $amount")
      result.foreach { roundupAmount =>
        assertEquals(
          roundupAmount,
          expectedRoundup,
          s"Wrong roundup for amount $amount"
        )
      }
    }
  }

  test(
    "validateTransactions should handle case-insensitive Starling detection"
  ) {
    val transactions = List(
      createValidTransaction(435L), // Valid - regular merchant
      createValidTransaction(
        250L,
        counterPartyType = "PAYEE",
        counterPartyName = Some("STARLING BANK")
      ), // Invalid - uppercase
      createValidTransaction(
        300L,
        counterPartyType = "PAYEE",
        counterPartyName = Some("starling bank")
      ), // Invalid - lowercase
      createValidTransaction(
        150L,
        counterPartyType = "PAYEE",
        counterPartyName = Some("Starling")
      ) // Invalid - partial match
    )

    val result = validator.validateTransactions(transactions, mainCategoryUid)

    assertEquals(result.isRight, true)
    result.foreach { validTxs =>
      assertEquals(validTxs.length, 1)
      assertEquals(validTxs.head.amount.minorUnits, 435L)
    }
  }

  test(
    "validateTransactions should allow non-PAYEE counterPartyType with Starling name"
  ) {
    val transactions = List(
      createValidTransaction(
        435L,
        counterPartyType = "MERCHANT",
        counterPartyName = Some("Starling Coffee Shop")
      ) // Valid - not PAYEE type
    )

    val result = validator.validateTransactions(transactions, mainCategoryUid)

    assertEquals(result.isRight, true)
    result.foreach { validTxs =>
      assertEquals(validTxs.length, 1)
      assertEquals(validTxs.head.amount.minorUnits, 435L)
    }
  }
}
