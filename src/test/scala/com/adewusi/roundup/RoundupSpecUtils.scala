package com.adewusi.roundup

import cats.effect.IO
import com.adewusi.roundup.clients._
import com.adewusi.roundup.domain.{AccountSelector, TransactionValidator}
import com.adewusi.roundup.model._
import com.adewusi.roundup.repository._
import com.adewusi.roundup.services.GoalService

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

trait RoundupSpecUtils {
  def testConfig(
      accessToken: String = "fake-token",
      baseUrl: String = "http://fake-api",
      initialGoalId: Option[UUID] = None
  ): AppConfig =
    AppConfig(
      starling = StarlingConfig(
        accessToken = accessToken,
        baseUrl = baseUrl,
        initialGoalId = initialGoalId
      )
    )

  def testAccount(
      accountType: String = "PRIMARY",
      currency: String = "GBP",
      createdAt: String = "2024-01-01T00:00:00Z",
      name: String = "Personal"
  ): Account =
    Account(
      accountUid = UUID.randomUUID(),
      accountType = accountType,
      defaultCategory = UUID.randomUUID(),
      currency = currency,
      createdAt = createdAt,
      name = name
    )

  def createTransactionFeedItem(
      amount: Long,
      currency: String = "GBP",
      direction: String = "OUT",
      status: String = "SETTLED",
      counterPartyName: Option[String] = Some("Test Merchant")
  ): TransactionFeedItem = {
    TransactionFeedItem(
      feedItemUid = UUID.randomUUID(),
      categoryUid = UUID.randomUUID(),
      amount = CurrencyAndAmount(currency, amount),
      sourceAmount = CurrencyAndAmount(currency, amount),
      direction = direction,
      updatedAt = ZonedDateTime.now(),
      transactionTime = ZonedDateTime.now(),
      settlementTime = None,
      retryAllocationUntilTime = None,
      source = "MASTER_CARD",
      sourceSubType = None,
      status = status,
      transactingApplicationUserUid = None,
      counterPartyType = "MERCHANT",
      counterPartyUid = None,
      counterPartyName = counterPartyName,
      counterPartySubEntityUid = None,
      counterPartySubEntityName = None,
      counterPartySubEntityIdentifier = None,
      counterPartySubEntitySubIdentifier = None,
      exchangeRate = None,
      totalFees = None,
      totalFeeAmount = None,
      reference = None,
      country = "GB",
      spendingCategory = "GENERAL",
      userNote = None,
      roundUp = None,
      hasAttachment = None,
      hasReceipt = None,
      batchPaymentDetails = None
    )
  }

  def createMockAccountClient(
      accountsResponse: IO[Either[AppError, List[Account]]]
  ): AccountClient[IO] = new AccountClient[IO] {
    override def fetchAccounts: IO[Either[AppError, List[Account]]] =
      accountsResponse
  }

  def createMockTransactionClient(
      transactionsResponse: IO[Either[AppError, List[TransactionFeedItem]]]
  ): TransactionClient[IO] = new TransactionClient[IO] {
    def fetchTransactions(
        account: Account,
        from: LocalDate
    ): IO[Either[AppError, List[TransactionFeedItem]]] = transactionsResponse
  }

  def createMockSavingsClient(
      getGoalResponse: IO[Either[AppError, SavingsGoal]],
      createGoalResponse: IO[Either[AppError, UUID]],
      transferResponse: IO[Either[AppError, AddMoneyResponse]]
  ): SavingsGoalClient[IO] = new SavingsGoalClient[IO] {

    override def getGoal(
        goal: UUID,
        accountUid: UUID
    ): IO[Either[AppError, SavingsGoal]] = getGoalResponse

    override def createGoal(accountUid: UUID): IO[Either[AppError, UUID]] =
      createGoalResponse

    override def transferToGoal(
        accountUid: UUID,
        goal: UUID,
        amountMinorUnits: Long
    ): IO[Either[AppError, AddMoneyResponse]] = transferResponse
  }

  def createMockGoalRepo(
      persistResponse: IO[Either[AppError, Unit]] = IO.pure(Right(())),
      readResponse: IO[Either[AppError, Option[UUID]]] = IO.pure(Right(None))
  ): GoalRepository[IO] = new GoalRepository[IO] {
    def persistGoal(goalId: UUID): IO[Either[AppError, Unit]] = persistResponse

    override def readGoal(
        config: AppConfig
    ): IO[Either[AppError, Option[UUID]]] = readResponse
  }

  def createMockGoalService(
      getOrCreateResponse: IO[Either[AppError, UUID]]
  ): GoalService[IO] = new GoalService[IO] {
    def getOrCreateGoal(
        config: AppConfig,
        accountUid: UUID
    ): IO[Either[AppError, UUID]] = getOrCreateResponse
  }

  def createMockTransferRepo(
      eligibilityResponse: IO[Either[AppError, Boolean]] = IO.pure(Right(true)),
      recordResponse: IO[Either[AppError, Unit]] = IO.pure(Right(()))
  ): TransferRepository[IO] = new TransferRepository[IO] {
    override def isEligibleForTransfer(
        goal: UUID,
        startDate: LocalDate,
        amountMinorUnits: Long
    ): IO[Either[AppError, Boolean]] = eligibilityResponse

    override def recordTransfer(
        goal: UUID,
        startDate: LocalDate,
        amountMinorUnits: Long,
        transfer: AddMoneyResponse
    ): IO[Either[AppError, Unit]] = recordResponse
  }

  def createMockAccountSelector(
      accountResponse: Either[AppError, Account] = Right(testAccount())
  ): AccountSelector = new AccountSelector {
    override def getCorrectAccount(
        accounts: List[Account]
    ): Either[AppError, Account] = accountResponse
  }

  def createMockTransactionValidator(
      validateTransactionsResponse: Either[AppError, List[
        TransactionFeedItem
      ]],
      validateRoundupResponse: Either[AppError, Long]
  ): TransactionValidator = new TransactionValidator {
    override def validateTransactions(
        transactions: List[TransactionFeedItem], mainCategoryUid: UUID
    ): Either[AppError, List[TransactionFeedItem]] =
      validateTransactionsResponse

    override def validateRoundupAmount(
        transactions: List[TransactionFeedItem], mainCategoryUid: UUID
    ): Either[AppError, Long] = validateRoundupResponse
  }
}
