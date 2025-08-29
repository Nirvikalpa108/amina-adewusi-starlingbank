package com.adewusi.roundup

import cats.effect._
import com.adewusi.roundup.clients._
import com.adewusi.roundup.domain.{AccountSelector, TransactionValidator}
import com.adewusi.roundup.model._
import com.adewusi.roundup.repository._
import com.adewusi.roundup.services.{GoalService, RoundupService}
import munit.CatsEffectSuite

import java.time.LocalDate
import java.util.UUID

class RoundupServiceSpec extends CatsEffectSuite with RoundupSpecUtils {

  private val startDate = LocalDate.of(2025, 1, 1)
  private val accounts = AccountsResponse(List(testAccount()))
  private val transactions = List(
    createTransactionFeedItem(195), // £1.95 → roundup = 5p
    createTransactionFeedItem(250) // £2.50 → roundup = 50p
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

  // Test scenarios table
  case class TestScenario(
      name: String,
      accountClient: AccountClient[IO],
      transactionClient: TransactionClient[IO],
      savingsClient: SavingsGoalClient[IO],
      goalRepo: GoalRepository[IO],
      goalService: GoalService[IO],
      transferRepo: TransferRepository[IO],
      selector: AccountSelector,
      validator: TransactionValidator,
      expectedResult: Either[AppError, Unit]
  )

  private val testScenarios = List(
    TestScenario(
      name = "happy path - existing goal retrieved successfully",
      accountClient =
        createMockAccountClient(IO.pure(Right(accounts.accounts))),
      transactionClient =
        createMockTransactionClient(IO.pure(Right(transactions))),
      savingsClient = createMockSavingsClient(
        getGoalResponse = IO.pure(Right(goal)),
        createGoalResponse = IO.raiseError(
          new RuntimeException("createGoal should not be called")
        ),
        transferResponse =
          IO.pure(Right(AddMoneyResponse(UUID.randomUUID(), success = true)))
      ),
      goalRepo = createMockGoalRepo(
        persistResponse = IO.pure(Right(())),
        readResponse = IO.pure(Right(Some(goal.savingsGoalUid)))
      ),
      goalService = createMockGoalService(IO.pure(Right(goal.savingsGoalUid))),
      transferRepo = createMockTransferRepo(
        eligibilityResponse = IO.pure(Right(true)),
        recordResponse = IO.pure(Right(()))
      ),
      selector = createMockAccountSelector(Right(testAccount())),
      validator = createMockTransactionValidator(
        validateTransactionsResponse = Right(transactions),
        validateRoundupResponse = Right(expectedRoundup.minorUnits)
      ),
      expectedResult = Right(())
    ),
    TestScenario(
      name = "account client fails",
      accountClient = createMockAccountClient(
        IO.pure(Left(GenericError("Account fetch failed")))
      ),
      transactionClient =
        createMockTransactionClient(IO.pure(Right(transactions))),
      savingsClient = createMockSavingsClient(
        getGoalResponse = IO.pure(Right(goal)),
        transferResponse =
          IO.pure(Right(AddMoneyResponse(UUID.randomUUID(), success = true))),
        createGoalResponse = IO.pure(Right(UUID.randomUUID()))
      ),
      goalRepo = createMockGoalRepo(
        persistResponse = IO.pure(Right(())),
        readResponse = IO.pure(Right(Some(goal.savingsGoalUid)))
      ),
      goalService = createMockGoalService(IO.pure(Right(goal.savingsGoalUid))),
      transferRepo = createMockTransferRepo(
        eligibilityResponse = IO.pure(Right(true)),
        recordResponse = IO.pure(Right(()))
      ),
      selector = createMockAccountSelector(Right(testAccount())),
      validator = createMockTransactionValidator(
        validateTransactionsResponse = Right(transactions),
        validateRoundupResponse = Right(expectedRoundup.minorUnits)
      ),
      expectedResult = Left(GenericError("Account fetch failed"))
    ),
    TestScenario(
      name = "transaction client fails",
      accountClient =
        createMockAccountClient(IO.pure(Right(accounts.accounts))),
      transactionClient = createMockTransactionClient(
        IO.pure(Left(GenericError("Transaction fetch failed")))
      ),
      savingsClient = createMockSavingsClient(
        getGoalResponse = IO.pure(Right(goal)),
        transferResponse =
          IO.pure(Right(AddMoneyResponse(UUID.randomUUID(), success = true))),
        createGoalResponse = IO.pure(Right(UUID.randomUUID()))
      ),
      goalRepo = createMockGoalRepo(
        persistResponse = IO.pure(Right(())),
        readResponse = IO.pure(Right(Some(goal.savingsGoalUid)))
      ),
      goalService = createMockGoalService(IO.pure(Right(goal.savingsGoalUid))),
      transferRepo = createMockTransferRepo(
        eligibilityResponse = IO.pure(Right(true)),
        recordResponse = IO.pure(Right(()))
      ),
      selector = createMockAccountSelector(Right(testAccount())),
      validator = createMockTransactionValidator(
        validateTransactionsResponse = Right(transactions),
        validateRoundupResponse = Right(expectedRoundup.minorUnits)
      ),
      expectedResult = Left(GenericError("Transaction fetch failed"))
    ),
    TestScenario(
      name = "goal service fails",
      accountClient =
        createMockAccountClient(IO.pure(Right(accounts.accounts))),
      transactionClient =
        createMockTransactionClient(IO.pure(Right(transactions))),
      savingsClient = createMockSavingsClient(
        getGoalResponse = IO.pure(Right(goal)),
        transferResponse =
          IO.pure(Right(AddMoneyResponse(UUID.randomUUID(), success = true))),
        createGoalResponse = IO.pure(Right(UUID.randomUUID()))
      ),
      goalRepo = createMockGoalRepo(
        persistResponse = IO.pure(Right(())),
        readResponse = IO.pure(Right(Some(goal.savingsGoalUid)))
      ),
      goalService = createMockGoalService(
        IO.pure(Left(NotFoundError("Goal service failed")))
      ),
      transferRepo = createMockTransferRepo(
        eligibilityResponse = IO.pure(Right(true)),
        recordResponse = IO.pure(Right(()))
      ),
      selector = createMockAccountSelector(Right(testAccount())),
      validator = createMockTransactionValidator(
        validateTransactionsResponse = Right(transactions),
        validateRoundupResponse = Right(expectedRoundup.minorUnits)
      ),
      expectedResult = Left(NotFoundError("Goal service failed"))
    ),
    TestScenario(
      name = "transfer repository eligibility check fails",
      accountClient =
        createMockAccountClient(IO.pure(Right(accounts.accounts))),
      transactionClient =
        createMockTransactionClient(IO.pure(Right(transactions))),
      savingsClient = createMockSavingsClient(
        getGoalResponse = IO.pure(Right(goal)),
        transferResponse =
          IO.pure(Right(AddMoneyResponse(UUID.randomUUID(), success = true))),
        createGoalResponse = IO.pure(Right(UUID.randomUUID()))
      ),
      goalRepo = createMockGoalRepo(
        persistResponse = IO.pure(Right(())),
        readResponse = IO.pure(Right(Some(goal.savingsGoalUid)))
      ),
      goalService = createMockGoalService(IO.pure(Right(goal.savingsGoalUid))),
      transferRepo = createMockTransferRepo(
        eligibilityResponse =
          IO.pure(Left(AlreadyTransferred("Transfer not eligible"))),
        recordResponse = IO.pure(Right(()))
      ),
      selector = createMockAccountSelector(Right(testAccount())),
      validator = createMockTransactionValidator(
        validateTransactionsResponse = Right(transactions),
        validateRoundupResponse = Right(expectedRoundup.minorUnits)
      ),
      expectedResult = Left(AlreadyTransferred("Transfer not eligible"))
    ),
    TestScenario(
      name = "idempotency: eligibility false prevents transfer",
      accountClient = createMockAccountClient(IO.pure(Right(accounts.accounts))),
      transactionClient = createMockTransactionClient(IO.pure(Right(transactions))),
      savingsClient = createMockSavingsClient(
        getGoalResponse = IO.pure(Right(goal)),
        transferResponse = IO.raiseError(new RuntimeException("Transfer should NOT be attempted")),
        createGoalResponse = IO.pure(Right(goal.savingsGoalUid))
      ),
      goalRepo = createMockGoalRepo(
        persistResponse = IO.pure(Right(())),
        readResponse = IO.pure(Right(Some(goal.savingsGoalUid)))
      ),
      goalService = createMockGoalService(IO.pure(Right(goal.savingsGoalUid))),
      transferRepo = createMockTransferRepo(
        eligibilityResponse = IO.pure(Right(false)),
        recordResponse = IO.pure(Right(()))
      ),
      selector = createMockAccountSelector(Right(testAccount())),
      validator = createMockTransactionValidator(
        validateTransactionsResponse = Right(transactions),
        validateRoundupResponse = Right(expectedRoundup.minorUnits)
      ),
      expectedResult = Left(AlreadyTransferred("Roundup for 2025-01-01 already processed"))
    )
  )

  // Table-driven test execution
  testScenarios.foreach { scenario =>
    test(s"processRoundups: ${scenario.name}") {
      implicit val accountClient: AccountClient[IO] = scenario.accountClient
      implicit val transactionClient: TransactionClient[IO] =
        scenario.transactionClient
      implicit val savingsClient: SavingsGoalClient[IO] = scenario.savingsClient
      implicit val goalRepo: GoalRepository[IO] = scenario.goalRepo
      implicit val goalService: GoalService[IO] = scenario.goalService
      implicit val transferRepo: TransferRepository[IO] = scenario.transferRepo
      implicit val selector: AccountSelector = scenario.selector
      implicit val validator: TransactionValidator = scenario.validator

      val resultIO: IO[Either[AppError, Unit]] =
        RoundupService.processRoundups[IO](startDate, testConfig()).value

      resultIO.assertEquals(scenario.expectedResult)
    }
  }
}
