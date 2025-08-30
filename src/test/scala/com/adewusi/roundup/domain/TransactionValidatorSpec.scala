package com.adewusi.roundup.domain

import com.adewusi.roundup.model._
import munit.CatsEffectSuite
import java.time.ZonedDateTime
import java.util.UUID

class TransactionValidatorSpec extends CatsEffectSuite {

  private val mainCategoryUid =
    UUID.fromString("11111111-1111-1111-1111-111111111111")
  private val validator = TransactionValidator.impl

  private def createValidTransaction(
      amountMinorUnits: Long,
      modify: TransactionFeedItem => TransactionFeedItem = identity
  ): TransactionFeedItem = {
    val base = TransactionFeedItem(
      feedItemUid = UUID.fromString("22222222-2222-2222-2222-222222222222"),
      categoryUid = mainCategoryUid,
      amount = CurrencyAndAmount("GBP", amountMinorUnits),
      sourceAmount = CurrencyAndAmount("GBP", amountMinorUnits),
      direction = "OUT",
      updatedAt = ZonedDateTime.now(),
      transactionTime = ZonedDateTime.now(),
      settlementTime = Some(ZonedDateTime.now()),
      retryAllocationUntilTime = None,
      source = "FASTER_PAYMENTS_IN",
      sourceSubType = None,
      status = "SETTLED",
      transactingApplicationUserUid = None,
      counterPartyType = "MERCHANT",
      counterPartyUid = None,
      counterPartyName = Some("Test Merchant"),
      counterPartySubEntityUid = None,
      counterPartySubEntityName = None,
      counterPartySubEntityIdentifier = None,
      counterPartySubEntitySubIdentifier = None,
      exchangeRate = None,
      totalFees = None,
      totalFeeAmount = None,
      reference = Some("Test transaction"),
      country = "GB",
      spendingCategory = "GENERAL",
      userNote = None,
      roundUp = None,
      hasAttachment = None,
      hasReceipt = None,
      batchPaymentDetails = None
    )
    modify(base)
  }

  /** Helper to assert that the result contains exactly one valid transaction
    * with the expected amount.
    */
  private def assertSingleValid(
      result: Either[_, List[TransactionFeedItem]],
      expectedAmount: Long
  ): Unit =
    result match {
      case Right(List(tx)) => assertEquals(tx.amount.minorUnits, expectedAmount)
      case other => fail(s"Expected single valid transaction but got $other")
    }

  /** Helper to assert that the result is a Left with NoTransactions error.
    */
  private def assertNoTxError(result: Either[AppError, _]): Unit =
    result match {
      case Left(NoTransactions) => ()
      case Left(other) => fail(s"Expected NoTransactions error but got $other")
      case Right(_)    => fail("Expected Left but got Right")
    }

  test(
    "validateTransactions returns all valid transactions when all eligible"
  ) {
    val txs = List(435L, 250L, 100L).map(createValidTransaction(_))
    val result = validator.validateTransactions(txs, mainCategoryUid)
    assert(result.isRight)
    result.foreach(txs => assertEquals(txs.length, 3))
  }

  test("calculateRoundupForTransaction calculates correct amounts") {
    val cases = List(
      435L -> 65L,
      250L -> 50L,
      199L -> 1L,
      500L -> 0L,
      1L -> 99L,
      999L -> 1L,
      1000L -> 0L
    )
    cases.foreach { case (amt, expected) =>
      val tx = createValidTransaction(amt)
      assertEquals(
        validator.validateRoundupAmount(List(tx), mainCategoryUid),
        Right(expected)
      )
    }
  }

  test("validateRoundupAmount returns 0 for whole pounds") {
    val txs = List(500L, 1000L).map(createValidTransaction(_))
    assertEquals(
      validator.validateRoundupAmount(txs, mainCategoryUid),
      Right(0L)
    )
  }

  // Parameterized filtering tests
  val filters = List[(String, TransactionFeedItem => TransactionFeedItem)](
    "direction" -> (_.copy(direction = "IN")),
    "currency" -> (tx =>
      tx.copy(
        amount = CurrencyAndAmount("USD", 250L),
        sourceAmount = CurrencyAndAmount("USD", 250L)
      )
    ),
    "status" -> (_.copy(status = "PENDING")),
    "categoryUid" -> (_.copy(categoryUid =
      UUID.fromString("33333333-3333-3333-3333-333333333333")
    )),
    "settlementTime" -> (_.copy(settlementTime = None)),
    "roundUp" -> (_.copy(roundUp =
      Some(
        AssociatedFeedRoundUp(UUID.randomUUID(), CurrencyAndAmount("GBP", 50L))
      )
    )),
    "totalFeeAmount" -> (_.copy(totalFeeAmount =
      Some(CurrencyAndAmount("GBP", 50L))
    )),
    "spendingCategory BANK_CHARGE" -> (_.copy(spendingCategory =
      "BANK_CHARGE"
    )),
    "spendingCategory INTEREST_PAYMENT" -> (_.copy(spendingCategory =
      "INTEREST_PAYMENT"
    )),
    "internal Starling transfer" -> (_.copy(
      counterPartyType = "PAYEE",
      counterPartyName = Some("Starling Bank")
    )),
    "status ACCOUNT_CHECK" -> (_.copy(status = "ACCOUNT_CHECK"))
  )

  filters.foreach { case (name, mod) =>
    test(s"validateTransactions filters out invalid $name") {
      val valid = createValidTransaction(435L)
      val invalid = mod(valid)
      assertSingleValid(
        validator.validateTransactions(List(valid, invalid), mainCategoryUid),
        435L
      )
    }
  }

  test("validateTransactions returns NoTransactions error when none eligible") {
    val txs = List(
      createValidTransaction(250L, _.copy(direction = "IN")),
      createValidTransaction(
        300L,
        _.copy(
          amount = CurrencyAndAmount("USD", 300L),
          sourceAmount = CurrencyAndAmount("USD", 300L)
        )
      ),
      createValidTransaction(150L, _.copy(status = "PENDING"))
    )
    assertNoTxError(validator.validateTransactions(txs, mainCategoryUid))
  }

  test("validateTransactions returns NoTransactions error for empty list") {
    assertNoTxError(validator.validateTransactions(List.empty, mainCategoryUid))
  }

  test(
    "validateRoundupAmount returns NoTransactions error when no valid transactions"
  ) {
    val txs = List(
      createValidTransaction(250L, _.copy(direction = "IN")),
      createValidTransaction(
        300L,
        _.copy(
          amount = CurrencyAndAmount("USD", 300L),
          sourceAmount = CurrencyAndAmount("USD", 300L)
        )
      )
    )
    assertNoTxError(validator.validateRoundupAmount(txs, mainCategoryUid))
  }
}
