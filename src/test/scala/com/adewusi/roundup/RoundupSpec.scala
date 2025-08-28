package com.adewusi.roundup

import cats.effect._
import com.adewusi.roundup.clients._
import com.adewusi.roundup.domain.{AccountSelector, TransactionValidator}
import com.adewusi.roundup.model._
import com.adewusi.roundup.repository._
import com.adewusi.roundup.services.GoalService
import munit.CatsEffectSuite

import java.time.LocalDate
import java.util.UUID

class RoundupSpec extends CatsEffectSuite with RoundupSpecUtils {

  private val startDate = LocalDate.of(2025, 1, 1)

  private val accounts = AccountsResponse(List(testAccount()))

  private val transactions = List(
    createTransactionFeedItem(195), // £1.95 → roundup = 5p
    createTransactionFeedItem(250)  // £2.50 → roundup = 50p
  )

  private val expectedRoundup = CurrencyAndAmount("GBP", 55) // 5p + 50p = 55p

  val goal = SavingsGoal(
    savingsGoalUid = UUID.fromString("182213A4-0D82-4419-905D-56DA33AEA420"),
    name = "Holiday Fund",
    target = None,
    totalSaved = CurrencyAndAmount("GBP", 0),
    savedPercentage = None,
    state = "ACTIVE"
  )

  // Mocked Starling accounts client: always returns a single test account
  implicit val accountClient: AccountClient[IO] =
    createMockAccountClient(IO.pure(Right(accounts.accounts)))

  // Mocked transactions client: always returns the two test transactions
  implicit val transactionClient: TransactionClient[IO] =
    createMockTransactionClient(IO.pure(Right(transactions)))

  // Simulates an existing active goal: `getGoal` returns the test goal.
  // `createGoal` should never be called so it can just return an error if invoked.
  // `transferToGoal` always succeeds to complete the round‑up flow.
  implicit val savingsClient: SavingsGoalClient[IO] =
  createMockSavingsClient(
    getGoalResponse    = IO.pure(Right(goal)),
    createGoalResponse = IO.raiseError(new RuntimeException("createGoal should not be called")),
    transferResponse   = IO.pure(Right(AddMoneyResponse(UUID.randomUUID(), success = true)))
  )

  // Mocked goal repository (persistence layer): happy path
  // readGoal` returns the saved goal, `persistGoal` is a no-op
  implicit val goalRepo: GoalRepository[IO] =
  createMockGoalRepo(
    persistResponse = IO.pure(Right(())),
    readResponse    = IO.pure(Right(Some(goal.savingsGoalUid)))
  )

  // Mocked goal service: always returns the existing goal UUID (retrieval path)
  implicit val goalService: GoalService[IO] =
    createMockGoalService(IO.pure(Right(goal.savingsGoalUid)))

  // Mocked transfer repository: happy path
  // - isEligibleForTransfer always true
  // - recordTransfer always succeeds
  implicit val transferRepo: TransferRepository[IO] =
  createMockTransferRepo(
    eligibilityResponse = IO.pure(Right(true)),
    recordResponse      = IO.pure(Right(()))
  )

  // Mocked account selector: always picks the test account as "the correct one"
  implicit val selector: AccountSelector =
    createMockAccountSelector(Right(testAccount()))

  // Mocked transaction validator: accepts all transactions and returns the expected roundup
  implicit val validator: TransactionValidator =
    createMockTransactionValidator(
      validateTransactionsResponse = Right(transactions),
      validateRoundupResponse      = Right(expectedRoundup.minorUnits)
    )

  test("processRoundups succeeds on happy path") {
    val resultIO: IO[Either[AppError, Unit]] =
      Roundup.processRoundups[IO](startDate, testConfig()).value

    resultIO.assertEquals(Right(()))
  }
}