package com.adewusi.roundup.clients

import cats.effect.IO
import com.adewusi.roundup.model._
import com.adewusi.roundup.starlingapis.StarlingTransactionApi
import munit.CatsEffectSuite

import java.time.{LocalDate, ZoneOffset, ZonedDateTime}
import java.util.UUID

class TransactionClientSpec extends CatsEffectSuite {

  val testAccountUid = UUID.fromString("11111111-1111-1111-1111-111111111111")
  val testAccount: Account = Account(
    accountUid = testAccountUid,
    accountType = "PRIMARY",
    defaultCategory = UUID.randomUUID(),
    currency = "GBP",
    createdAt = "2023-01-01T00:00:00Z",
    name = "Main Account"
  )

  val testStartDate = LocalDate.of(2023, 10, 1)
  val expectedStartUtc: ZonedDateTime =
    testStartDate.atStartOfDay(ZoneOffset.UTC)
  val expectedEndUtc: ZonedDateTime = expectedStartUtc.plusDays(7)

  val sampleTransaction = TransactionFeedItem(
    feedItemUid = UUID.randomUUID(),
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
  ): StarlingTransactionApi[IO] = {
    new StarlingTransactionApi[IO] {
      def getSettledTransactionsBetween(
          accountUid: UUID,
          minTransactionTimestamp: ZonedDateTime,
          maxTransactionTimestamp: ZonedDateTime
      ): IO[TransactionFeedResponse] = {
        accountUidAssertion(accountUid)
        dateRangeAssertion(minTransactionTimestamp, maxTransactionTimestamp)
        response
      }
    }
  }

  test("fetchTransactions returns Right with transactions when API succeeds") {
    implicit val mockApi = createMockApi(
      response = IO.pure(TransactionFeedResponse(List(sampleTransaction))),
      accountUidAssertion =
        accountUid => assertEquals(accountUid, testAccountUid),
      dateRangeAssertion = (min, max) => {
        assertEquals(min, expectedStartUtc)
        assertEquals(max, expectedEndUtc)
      }
    )

    val client = TransactionClient.impl[IO]

    assertIO(
      client.fetchTransactions(testAccount, testStartDate),
      Right(List(sampleTransaction))
    )
  }

  test(
    "fetchTransactions returns Right with empty list when API returns empty response"
  ) {
    implicit val mockApi: StarlingTransactionApi[IO] = createMockApi()
    val client = TransactionClient.impl[IO]

    assertIO(
      client.fetchTransactions(testAccount, testStartDate),
      Right(List.empty)
    )
  }

  test(
    "fetchTransactions returns multiple transactions when API returns multiple items"
  ) {
    val transaction2 = sampleTransaction.copy(
      feedItemUid = UUID.randomUUID(),
      amount = CurrencyAndAmount("GBP", 2500),
      counterPartyName = Some("Sainsbury's")
    )

    implicit val mockApi = createMockApi(
      response =
        IO.pure(TransactionFeedResponse(List(sampleTransaction, transaction2)))
    )

    val client = TransactionClient.impl[IO]

    assertIO(
      client.fetchTransactions(testAccount, testStartDate),
      Right(List(sampleTransaction, transaction2))
    )
  }

  test("fetchTransactions returns Left with GenericError when API fails") {
    val apiError = new RuntimeException("Network timeout")
    implicit val mockApi = createMockApi(response = IO.raiseError(apiError))
    val client = TransactionClient.impl[IO]

    assertIO(
      client.fetchTransactions(testAccount, testStartDate),
      Left(GenericError("Failed to fetch transactions: Network timeout"))
    )
  }

  test("fetchTransactions calculates correct 7-day date range in UTC") {
    implicit val mockApi = createMockApi(
      dateRangeAssertion = (min, max) => {
        assertEquals(min, expectedStartUtc)
        assertEquals(max, expectedEndUtc)
      }
    )

    val client = TransactionClient.impl[IO]

    assertIO(
      client.fetchTransactions(testAccount, testStartDate),
      Right(List.empty)
    )
  }

  test("fetchTransactions passes correct account UID to API") {
    val differentUid = UUID.fromString("22222222-2222-2222-2222-222222222222")
    val differentAccount = testAccount.copy(accountUid = differentUid)

    implicit val mockApi = createMockApi(
      accountUidAssertion = accountUid => assertEquals(accountUid, differentUid)
    )

    val client = TransactionClient.impl[IO]

    assertIO(
      client.fetchTransactions(differentAccount, testStartDate),
      Right(List.empty)
    )
  }

  test("fetchTransactions handles different start dates correctly") {
    val differentStartDate = LocalDate.of(2023, 12, 15)
    val expectedStart = differentStartDate.atStartOfDay(ZoneOffset.UTC)
    val expectedEnd = expectedStart.plusDays(7)

    implicit val mockApi = createMockApi(
      dateRangeAssertion = (min, max) => {
        assertEquals(min, expectedStart)
        assertEquals(max, expectedEnd)
      }
    )

    val client = TransactionClient.impl[IO]

    assertIO(
      client.fetchTransactions(testAccount, differentStartDate),
      Right(List.empty)
    )
  }

  test("fetchTransactions handles leap year date correctly") {
    val leapYearDate = LocalDate.of(2024, 2, 29)
    val expectedStart = leapYearDate.atStartOfDay(ZoneOffset.UTC)
    val expectedEnd = expectedStart.plusDays(7)

    implicit val mockApi = createMockApi(
      dateRangeAssertion = (min, max) => {
        assertEquals(min, expectedStart)
        assertEquals(max, expectedEnd)
        assertEquals(max.toLocalDate, LocalDate.of(2024, 3, 7))
      }
    )

    val client = TransactionClient.impl[IO]
    assertIO(
      client.fetchTransactions(testAccount, leapYearDate),
      Right(List.empty)
    )
  }

  test("fetchTransactions handles month boundary crossing correctly") {
    val endOfMonth = LocalDate.of(2023, 1, 31)
    val expectedStart = endOfMonth.atStartOfDay(ZoneOffset.UTC)
    val expectedEnd = expectedStart.plusDays(7)

    implicit val mockApi = createMockApi(
      dateRangeAssertion = (min, max) => {
        assertEquals(min, expectedStart)
        assertEquals(max, expectedEnd)
        assertEquals(max.toLocalDate, LocalDate.of(2023, 2, 7))
      }
    )

    val client = TransactionClient.impl[IO]
    assertIO(
      client.fetchTransactions(testAccount, endOfMonth),
      Right(List.empty)
    )
  }

  test("fetchTransactions handles year boundary crossing correctly") {
    val endOfYear = LocalDate.of(2023, 12, 31)
    val expectedStart = endOfYear.atStartOfDay(ZoneOffset.UTC)
    val expectedEnd = expectedStart.plusDays(7)

    implicit val mockApi = createMockApi(
      dateRangeAssertion = (min, max) => {
        assertEquals(min, expectedStart)
        assertEquals(max, expectedEnd)
        assertEquals(max.toLocalDate, LocalDate.of(2024, 1, 7))
        assertEquals(max.getYear, 2024)
      }
    )

    val client = TransactionClient.impl[IO]
    assertIO(
      client.fetchTransactions(testAccount, endOfYear),
      Right(List.empty)
    )
  }

  test("fetchTransactions handles February in non-leap year correctly") {
    val febInNonLeapYear = LocalDate.of(2023, 2, 25)
    val expectedStart = febInNonLeapYear.atStartOfDay(ZoneOffset.UTC)
    val expectedEnd = expectedStart.plusDays(7)

    implicit val mockApi = createMockApi(
      dateRangeAssertion = (min, max) => {
        assertEquals(min, expectedStart)
        assertEquals(max, expectedEnd)
        assertEquals(max.toLocalDate, LocalDate.of(2023, 3, 4))
      }
    )

    val client = TransactionClient.impl[IO]
    assertIO(
      client.fetchTransactions(testAccount, febInNonLeapYear),
      Right(List.empty)
    )
  }

  test(
    "fetchTransactions ensures UTC timezone is used regardless of system timezone"
  ) {
    val testDate = LocalDate.of(2023, 6, 15)
    val expectedStart = testDate.atStartOfDay(ZoneOffset.UTC)
    val expectedEnd = expectedStart.plusDays(7)

    implicit val mockApi = createMockApi(
      dateRangeAssertion = (min, max) => {
        assertEquals(min.getOffset, ZoneOffset.UTC)
        assertEquals(max.getOffset, ZoneOffset.UTC)
        assertEquals(min, expectedStart)
        assertEquals(max, expectedEnd)
      }
    )

    val client = TransactionClient.impl[IO]
    assertIO(client.fetchTransactions(testAccount, testDate), Right(List.empty))
  }

  test("fetchTransactions handles minimum possible LocalDate") {
    val minDate = LocalDate.of(1970, 1, 1)
    val expectedStart = minDate.atStartOfDay(ZoneOffset.UTC)
    val expectedEnd = expectedStart.plusDays(7)

    implicit val mockApi = createMockApi(
      dateRangeAssertion = (min, max) => {
        assertEquals(min, expectedStart)
        assertEquals(max, expectedEnd)
        assertEquals(max.toLocalDate, LocalDate.of(1970, 1, 8))
      }
    )

    val client = TransactionClient.impl[IO]
    assertIO(client.fetchTransactions(testAccount, minDate), Right(List.empty))
  }

  test("fetchTransactions handles far future date") {
    val futureDate = LocalDate.of(2099, 12, 25)
    val expectedStart = futureDate.atStartOfDay(ZoneOffset.UTC)
    val expectedEnd = expectedStart.plusDays(7)

    implicit val mockApi = createMockApi(
      dateRangeAssertion = (min, max) => {
        assertEquals(min, expectedStart)
        assertEquals(max, expectedEnd)
        assertEquals(max.toLocalDate, LocalDate.of(2100, 1, 1))
      }
    )

    val client = TransactionClient.impl[IO]
    assertIO(
      client.fetchTransactions(testAccount, futureDate),
      Right(List.empty)
    )
  }

  test("fetchTransactions handles daylight saving time transition dates") {
    val dstTransitionDate = LocalDate.of(2023, 3, 26)
    val expectedStart = dstTransitionDate.atStartOfDay(ZoneOffset.UTC)
    val expectedEnd = expectedStart.plusDays(7)

    implicit val mockApi = createMockApi(
      dateRangeAssertion = (min, max) => {
        assertEquals(min.getOffset, ZoneOffset.UTC)
        assertEquals(max.getOffset, ZoneOffset.UTC)
        assertEquals(min, expectedStart)
        assertEquals(max, expectedEnd)
      }
    )

    val client = TransactionClient.impl[IO]
    assertIO(
      client.fetchTransactions(testAccount, dstTransitionDate),
      Right(List.empty)
    )
  }

  test("fetchTransactions maintains exact 7-day window") {
    val testDate = LocalDate.of(2023, 5, 15)
    val expectedStart = testDate.atStartOfDay(ZoneOffset.UTC)
    val expectedEnd = expectedStart.plusDays(7)

    implicit val mockApi = createMockApi(
      dateRangeAssertion = (min, max) => {
        val duration = java.time.Duration.between(min, max)
        assertEquals(duration.toDays, 7L)
        assertEquals(duration.toHours, 168L)
        assertEquals(max, expectedEnd)
      }
    )

    val client = TransactionClient.impl[IO]
    assertIO(client.fetchTransactions(testAccount, testDate), Right(List.empty))
  }

  test("fetchTransactions handles start time precision correctly") {
    val testDate = LocalDate.of(2023, 8, 10)
    val expectedStart = testDate.atStartOfDay(ZoneOffset.UTC)

    implicit val mockApi = createMockApi(
      dateRangeAssertion = (min, _) => {
        assertEquals(min.getHour, 0)
        assertEquals(min.getMinute, 0)
        assertEquals(min.getSecond, 0)
        assertEquals(min.getNano, 0)
        assertEquals(min, expectedStart)
      }
    )

    val client = TransactionClient.impl[IO]
    assertIO(client.fetchTransactions(testAccount, testDate), Right(List.empty))
  }
}
