package com.adewusi.roundup.clients

import cats.effect.IO
import com.adewusi.roundup.TestUtils
import com.adewusi.roundup.model._
import com.adewusi.roundup.starlingapis.StarlingTransactionApi
import munit.CatsEffectSuite
import org.http4s.Uri

import java.time.{LocalDate, ZoneOffset, ZonedDateTime}
import java.util.UUID

class TransactionClientSpec extends CatsEffectSuite with TestUtils {

  val testAccountUid: UUID =
    UUID.fromString("11111111-1111-1111-1111-111111111111")
  val testAccount: Account = Account(
    accountUid = testAccountUid,
    accountType = "PRIMARY",
    defaultCategory = UUID.fromString("33333333-3333-3333-3333-333333333333"),
    currency = "GBP",
    createdAt = "2023-01-01T00:00:00Z",
    name = "Main Account"
  )

  val sampleTransaction: TransactionFeedItem = TransactionFeedItem(
    feedItemUid = UUID.fromString("44444444-4444-4444-4444-444444444444"),
    categoryUid = testAccount.defaultCategory,
    amount = CurrencyAndAmount("GBP", 1250),
    sourceAmount = CurrencyAndAmount("GBP", 1250),
    direction = "OUT",
    updatedAt = ZonedDateTime.now(),
    transactionTime = ZonedDateTime.now(),
    settlementTime = Some(ZonedDateTime.now()),
    retryAllocationUntilTime = None,
    source = "MASTER_CARD",
    sourceSubType = Some("CONTACTLESS"),
    status = "SETTLED",
    transactingApplicationUserUid = None,
    counterPartyType = "MERCHANT",
    counterPartyUid = None,
    counterPartyName = Some("Tesco"),
    counterPartySubEntityUid = None,
    counterPartySubEntityName = Some("Tesco Southampton"),
    counterPartySubEntityIdentifier = Some("608371"),
    counterPartySubEntitySubIdentifier = Some("01234567"),
    exchangeRate = None,
    totalFees = None,
    totalFeeAmount = None,
    reference = Some("TESCO-STORES-6148 SOUTHAMPTON GBR"),
    country = "GB",
    spendingCategory = "GROCERIES",
    userNote = None,
    roundUp = None,
    hasAttachment = Some(false),
    hasReceipt = Some(false),
    batchPaymentDetails = None
  )

  private def createMockApi(
      response: IO[TransactionFeedResponse] =
        IO.pure(TransactionFeedResponse(List.empty)),
      accountUidAssertion: UUID => Unit = _ => (),
      dateRangeAssertion: (ZonedDateTime, ZonedDateTime) => Unit = (_, _) => ()
  ): StarlingTransactionApi[IO] = new StarlingTransactionApi[IO] {
    override def getSettledTransactionsBetween(
        accountUid: UUID,
        minTransactionTimestamp: ZonedDateTime,
        maxTransactionTimestamp: ZonedDateTime,
        accessToken: String,
        baseUri: Uri
    ): IO[TransactionFeedResponse] = {
      accountUidAssertion(accountUid)
      dateRangeAssertion(minTransactionTimestamp, maxTransactionTimestamp)
      response
    }
  }

  // Helper for date range assertions
  private def assertDateRange(
      startDate: LocalDate,
      min: ZonedDateTime,
      max: ZonedDateTime
  ): Unit = {
    val expectedStart = startDate.atStartOfDay(ZoneOffset.UTC)
    val expectedEnd = expectedStart.plusDays(7)
    assertEquals(min, expectedStart)
    assertEquals(max, expectedEnd)
    assertEquals(min.getOffset, ZoneOffset.UTC)
    assertEquals(max.getOffset, ZoneOffset.UTC)
    val duration = java.time.Duration.between(min, max)
    assertEquals(duration.toDays, 7L)
  }

  test("fetchTransactions returns Right with transactions on API success") {
    val testStartDate = LocalDate.of(2023, 10, 1)
    implicit val mockApi: StarlingTransactionApi[IO] = createMockApi(
      response = IO.pure(TransactionFeedResponse(List(sampleTransaction))),
      accountUidAssertion =
        accountUid => assertEquals(accountUid, testAccountUid),
      dateRangeAssertion =
        (min, max) => assertDateRange(testStartDate, min, max)
    )

    val client = TransactionClient.impl[IO](testConfig)

    client
      .fetchTransactions(testAccount, testStartDate)
      .assertEquals(Right(List(sampleTransaction)))
  }

  test(
    "fetchTransactions returns Right with empty list when API returns no transactions"
  ) {
    val testStartDate = LocalDate.of(2023, 10, 1)
    implicit val mockApi: StarlingTransactionApi[IO] = createMockApi()
    val client = TransactionClient.impl[IO](testConfig)

    client
      .fetchTransactions(testAccount, testStartDate)
      .assertEquals(Right(List.empty))
  }

  test("fetchTransactions returns Left with GenericError when API fails") {
    val testStartDate = LocalDate.of(2023, 10, 1)
    val apiError = new RuntimeException("Network timeout")
    implicit val mockApi: StarlingTransactionApi[IO] =
      createMockApi(response = IO.raiseError(apiError))
    val client = TransactionClient.impl[IO](testConfig)

    client
      .fetchTransactions(testAccount, testStartDate)
      .assertEquals(
        Left(GenericError("Failed to fetch transactions: Network timeout"))
      )
  }

  test("fetchTransactions passes correct account UID to API") {
    val testStartDate = LocalDate.of(2023, 10, 1)
    val differentUid = UUID.fromString("22222222-2222-2222-2222-222222222222")
    val differentAccount = testAccount.copy(accountUid = differentUid)

    implicit val mockApi: StarlingTransactionApi[IO] = createMockApi(
      accountUidAssertion = accountUid => assertEquals(accountUid, differentUid)
    )

    val client = TransactionClient.impl[IO](testConfig)

    client
      .fetchTransactions(differentAccount, testStartDate)
      .assertEquals(Right(List.empty))
  }

  // Parameterized test for various date range calculations
  Seq(
    ("a standard date", LocalDate.of(2023, 10, 1)),
    ("a leap year date", LocalDate.of(2024, 2, 29)),
    ("a month boundary crossing date", LocalDate.of(2023, 1, 31)),
    ("a year boundary crossing date", LocalDate.of(2023, 12, 31)),
    ("February in a non-leap year", LocalDate.of(2023, 2, 25)),
    ("minimum possible LocalDate", LocalDate.of(1970, 1, 1)),
    ("far future date", LocalDate.of(2099, 12, 25)),
    ("daylight saving time transition date", LocalDate.of(2023, 3, 26))
  ).foreach { case (description, startDate) =>
    test(
      s"fetchTransactions calculates correct 7-day UTC date range for $description"
    ) {
      implicit val mockApi: StarlingTransactionApi[IO] = createMockApi(
        dateRangeAssertion = (min, max) => assertDateRange(startDate, min, max)
      )

      val client = TransactionClient.impl[IO](testConfig)

      client
        .fetchTransactions(testAccount, startDate)
        .assertEquals(Right(List.empty))
    }
  }
}
