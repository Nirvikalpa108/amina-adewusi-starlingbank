package com.adewusi.roundup

import cats.effect.IO
import com.adewusi.roundup.model._
import com.adewusi.roundup.starlingapis.StarlingTransactionApi
import munit.CatsEffectSuite

import java.time.{LocalDate, ZoneOffset, ZonedDateTime}
import java.util.UUID

class TransactionClientSpec extends CatsEffectSuite {

  val testAccountUid = "11111111-1111-1111-1111-111111111111"
  val testAccount: Account = Account(
    accountUid = testAccountUid,
    accountType = "PRIMARY",
    defaultCategory = UUID.randomUUID().toString,
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
    categoryUid = UUID.fromString(testAccount.defaultCategory),
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

  test("fetchTransactions returns Right with transactions when API succeeds") {
    val mockApi = new StarlingTransactionApi[IO] {
      def getSettledTransactionsBetween(
          accountUid: String,
          minTransactionTimestamp: ZonedDateTime,
          maxTransactionTimestamp: ZonedDateTime
      ): IO[TransactionFeedResponse] = {
        assertEquals(accountUid, testAccountUid)
        assertEquals(minTransactionTimestamp, expectedStartUtc)
        assertEquals(maxTransactionTimestamp, expectedEndUtc)
        IO.pure(TransactionFeedResponse(List(sampleTransaction)))
      }
    }

    val client = TransactionClient.impl[IO](mockApi)

    assertIO(
      client.fetchTransactions(testAccount, testStartDate),
      Right(List(sampleTransaction))
    )
  }

  test(
    "fetchTransactions returns Right with empty list when API returns empty response"
  ) {
    val mockApi = new StarlingTransactionApi[IO] {
      def getSettledTransactionsBetween(
          a: String,
          b: ZonedDateTime,
          c: ZonedDateTime
      ) =
        IO.pure(TransactionFeedResponse(List.empty))
    }

    val client = TransactionClient.impl[IO](mockApi)

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

    val mockApi = new StarlingTransactionApi[IO] {
      def getSettledTransactionsBetween(
          a: String,
          b: ZonedDateTime,
          c: ZonedDateTime
      ) =
        IO.pure(TransactionFeedResponse(List(sampleTransaction, transaction2)))
    }

    val client = TransactionClient.impl[IO](mockApi)

    assertIO(
      client.fetchTransactions(testAccount, testStartDate),
      Right(List(sampleTransaction, transaction2))
    )
  }

  test("fetchTransactions returns Left with GenericError when API fails") {
    val apiError = new RuntimeException("Network timeout")

    val mockApi = new StarlingTransactionApi[IO] {
      def getSettledTransactionsBetween(
          a: String,
          b: ZonedDateTime,
          c: ZonedDateTime
      ) =
        IO.raiseError(apiError)
    }

    val client = TransactionClient.impl[IO](mockApi)

    assertIO(
      client.fetchTransactions(testAccount, testStartDate),
      Left(GenericError("Failed to fetch transactions: Network timeout"))
    )
  }

  test("fetchTransactions calculates correct 7-day date range in UTC") {
    val mockApi = new StarlingTransactionApi[IO] {
      def getSettledTransactionsBetween(
          a: String,
          min: ZonedDateTime,
          max: ZonedDateTime
      ) = {
        assertEquals(min, expectedStartUtc)
        assertEquals(max, expectedEndUtc)
        IO.pure(TransactionFeedResponse(List.empty))
      }
    }

    val client = TransactionClient.impl[IO](mockApi)

    assertIO(
      client.fetchTransactions(testAccount, testStartDate),
      Right(List.empty)
    )
  }

  test("fetchTransactions passes correct account UID to API") {
    val differentUid = "22222222-2222-2222-2222-222222222222"
    val differentAccount = testAccount.copy(accountUid = differentUid)

    val mockApi = new StarlingTransactionApi[IO] {
      def getSettledTransactionsBetween(
          accountUid: String,
          b: ZonedDateTime,
          c: ZonedDateTime
      ) = {
        assertEquals(accountUid, differentUid)
        IO.pure(TransactionFeedResponse(List.empty))
      }
    }

    val client = TransactionClient.impl[IO](mockApi)

    assertIO(
      client.fetchTransactions(differentAccount, testStartDate),
      Right(List.empty)
    )
  }

  test("fetchTransactions handles different start dates correctly") {
    val differentStartDate = LocalDate.of(2023, 12, 15)
    val expectedStart = differentStartDate.atStartOfDay(ZoneOffset.UTC)
    val expectedEnd = expectedStart.plusDays(7)

    val mockApi = new StarlingTransactionApi[IO] {
      def getSettledTransactionsBetween(
          a: String,
          min: ZonedDateTime,
          max: ZonedDateTime
      ) = {
        assertEquals(min, expectedStart)
        assertEquals(max, expectedEnd)
        IO.pure(TransactionFeedResponse(List.empty))
      }
    }

    val client = TransactionClient.impl[IO](mockApi)

    assertIO(
      client.fetchTransactions(testAccount, differentStartDate),
      Right(List.empty)
    )
  }

  test("fetchTransactions handles leap year date correctly") {
    val leapYearDate = LocalDate.of(2024, 2, 29) // Feb 29th in leap year
    val expectedStart = leapYearDate.atStartOfDay(ZoneOffset.UTC)
    val expectedEnd = expectedStart.plusDays(7) // Should be March 7th, 2024

    val mockApi = new StarlingTransactionApi[IO] {
      def getSettledTransactionsBetween(
          a: String,
          min: ZonedDateTime,
          max: ZonedDateTime
      ) = {
        assertEquals(min, expectedStart)
        assertEquals(max, expectedEnd)
        assertEquals(max.toLocalDate, LocalDate.of(2024, 3, 7))
        IO.pure(TransactionFeedResponse(List.empty))
      }
    }

    val client = TransactionClient.impl[IO](mockApi)
    assertIO(
      client.fetchTransactions(testAccount, leapYearDate),
      Right(List.empty)
    )
  }

  test("fetchTransactions handles month boundary crossing correctly") {
    val endOfMonth = LocalDate.of(2023, 1, 31) // January 31st
    val expectedStart = endOfMonth.atStartOfDay(ZoneOffset.UTC)
    val expectedEnd = expectedStart.plusDays(7) // Should be February 7th

    val mockApi = new StarlingTransactionApi[IO] {
      def getSettledTransactionsBetween(
          a: String,
          min: ZonedDateTime,
          max: ZonedDateTime
      ) = {
        assertEquals(min, expectedStart)
        assertEquals(max, expectedEnd)
        assertEquals(max.toLocalDate, LocalDate.of(2023, 2, 7))
        IO.pure(TransactionFeedResponse(List.empty))
      }
    }

    val client = TransactionClient.impl[IO](mockApi)
    assertIO(
      client.fetchTransactions(testAccount, endOfMonth),
      Right(List.empty)
    )
  }

  test("fetchTransactions handles year boundary crossing correctly") {
    val endOfYear = LocalDate.of(2023, 12, 31) // December 31st
    val expectedStart = endOfYear.atStartOfDay(ZoneOffset.UTC)
    val expectedEnd = expectedStart.plusDays(7) // Should be January 7th, 2024

    val mockApi = new StarlingTransactionApi[IO] {
      def getSettledTransactionsBetween(
          a: String,
          min: ZonedDateTime,
          max: ZonedDateTime
      ) = {
        assertEquals(min, expectedStart)
        assertEquals(max, expectedEnd)
        assertEquals(max.toLocalDate, LocalDate.of(2024, 1, 7))
        assertEquals(max.getYear, 2024)
        IO.pure(TransactionFeedResponse(List.empty))
      }
    }

    val client = TransactionClient.impl[IO](mockApi)
    assertIO(
      client.fetchTransactions(testAccount, endOfYear),
      Right(List.empty)
    )
  }

  test("fetchTransactions handles February in non-leap year correctly") {
    val febInNonLeapYear =
      LocalDate.of(2023, 2, 25) // Feb 25th in non-leap year
    val expectedStart = febInNonLeapYear.atStartOfDay(ZoneOffset.UTC)
    val expectedEnd = expectedStart.plusDays(7) // Should be March 4th, 2023

    val mockApi = new StarlingTransactionApi[IO] {
      def getSettledTransactionsBetween(
          a: String,
          min: ZonedDateTime,
          max: ZonedDateTime
      ) = {
        assertEquals(min, expectedStart)
        assertEquals(max, expectedEnd)
        assertEquals(max.toLocalDate, LocalDate.of(2023, 3, 4))
        IO.pure(TransactionFeedResponse(List.empty))
      }
    }

    val client = TransactionClient.impl[IO](mockApi)
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

    val mockApi = new StarlingTransactionApi[IO] {
      def getSettledTransactionsBetween(
          a: String,
          min: ZonedDateTime,
          max: ZonedDateTime
      ) = {
        // Verify both timestamps are in UTC
        assertEquals(min.getOffset, ZoneOffset.UTC)
        assertEquals(max.getOffset, ZoneOffset.UTC)
        assertEquals(min, expectedStart)
        assertEquals(max, expectedEnd)
        IO.pure(TransactionFeedResponse(List.empty))
      }
    }

    val client = TransactionClient.impl[IO](mockApi)
    assertIO(client.fetchTransactions(testAccount, testDate), Right(List.empty))
  }

  test("fetchTransactions handles minimum possible LocalDate") {
    val minDate = LocalDate.of(1970, 1, 1) // Unix epoch start
    val expectedStart = minDate.atStartOfDay(ZoneOffset.UTC)
    val expectedEnd = expectedStart.plusDays(7)

    val mockApi = new StarlingTransactionApi[IO] {
      def getSettledTransactionsBetween(
          a: String,
          min: ZonedDateTime,
          max: ZonedDateTime
      ) = {
        assertEquals(min, expectedStart)
        assertEquals(max, expectedEnd)
        assertEquals(max.toLocalDate, LocalDate.of(1970, 1, 8))
        IO.pure(TransactionFeedResponse(List.empty))
      }
    }

    val client = TransactionClient.impl[IO](mockApi)
    assertIO(client.fetchTransactions(testAccount, minDate), Right(List.empty))
  }

  test("fetchTransactions handles far future date") {
    val futureDate = LocalDate.of(2099, 12, 25)
    val expectedStart = futureDate.atStartOfDay(ZoneOffset.UTC)
    val expectedEnd = expectedStart.plusDays(7)

    val mockApi = new StarlingTransactionApi[IO] {
      def getSettledTransactionsBetween(
          a: String,
          min: ZonedDateTime,
          max: ZonedDateTime
      ) = {
        assertEquals(min, expectedStart)
        assertEquals(max, expectedEnd)
        assertEquals(max.toLocalDate, LocalDate.of(2100, 1, 1))
        IO.pure(TransactionFeedResponse(List.empty))
      }
    }

    val client = TransactionClient.impl[IO](mockApi)
    assertIO(
      client.fetchTransactions(testAccount, futureDate),
      Right(List.empty)
    )
  }

  test("fetchTransactions handles daylight saving time transition dates") {
    // Test around typical DST transition dates (though we use UTC, so shouldn't matter)
    val dstTransitionDate = LocalDate.of(2023, 3, 26) // Typical EU DST start
    val expectedStart = dstTransitionDate.atStartOfDay(ZoneOffset.UTC)
    val expectedEnd = expectedStart.plusDays(7)

    val mockApi = new StarlingTransactionApi[IO] {
      def getSettledTransactionsBetween(
          a: String,
          min: ZonedDateTime,
          max: ZonedDateTime
      ) = {
        // Should still be UTC regardless of DST
        assertEquals(min.getOffset, ZoneOffset.UTC)
        assertEquals(max.getOffset, ZoneOffset.UTC)
        assertEquals(min, expectedStart)
        assertEquals(max, expectedEnd)
        IO.pure(TransactionFeedResponse(List.empty))
      }
    }

    val client = TransactionClient.impl[IO](mockApi)
    assertIO(
      client.fetchTransactions(testAccount, dstTransitionDate),
      Right(List.empty)
    )
  }

  test("fetchTransactions maintains exact 7-day window") {
    val testDate = LocalDate.of(2023, 5, 15)
    val expectedStart = testDate.atStartOfDay(ZoneOffset.UTC)
    val expectedEnd = expectedStart.plusDays(7)

    val mockApi = new StarlingTransactionApi[IO] {
      def getSettledTransactionsBetween(
          a: String,
          min: ZonedDateTime,
          max: ZonedDateTime
      ) = {
        val duration = java.time.Duration.between(min, max)
        assertEquals(duration.toDays, 7L)
        assertEquals(duration.toHours, 168L)

        assertEquals(max, expectedEnd)
        IO.pure(TransactionFeedResponse(List.empty))
      }
    }

    val client = TransactionClient.impl[IO](mockApi)
    assertIO(client.fetchTransactions(testAccount, testDate), Right(List.empty))
  }

  test("fetchTransactions handles start time precision correctly") {
    val testDate = LocalDate.of(2023, 8, 10)
    val expectedStart = testDate.atStartOfDay(ZoneOffset.UTC)

    val mockApi = new StarlingTransactionApi[IO] {
      def getSettledTransactionsBetween(
          a: String,
          min: ZonedDateTime,
          max: ZonedDateTime
      ) = {
        // Verify start time is exactly midnight UTC
        assertEquals(min.getHour, 0)
        assertEquals(min.getMinute, 0)
        assertEquals(min.getSecond, 0)
        assertEquals(min.getNano, 0)

        assertEquals(min, expectedStart)
        IO.pure(TransactionFeedResponse(List.empty))
      }
    }

    val client = TransactionClient.impl[IO](mockApi)
    assertIO(client.fetchTransactions(testAccount, testDate), Right(List.empty))
  }
}
