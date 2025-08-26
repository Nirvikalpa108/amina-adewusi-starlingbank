package com.adewusi.roundup

import cats.effect.kernel.Sync
import cats.implicits._
import com.adewusi.roundup.model._
import com.adewusi.roundup.starlingapis.StarlingSavingsGoalsApi

import java.util.UUID

trait SavingsGoalClient[F[_]] {
  def readGoalFromFile(config: AppConfig): F[Either[AppError, Option[UUID]]]
  def validateGoal(
      goalUuid: UUID,
      accountUid: UUID
  ): F[Either[AppError, SavingsGoal]]
  def createAndPersistGoal(
      config: AppConfig,
      accountUid: UUID
  ): F[Either[AppError, UUID]]

  def transferToSavingsGoal(
      config: AppConfig,
      accountUid: UUID,
      goal: UUID,
      amountMinorUnits: Long
  ): F[Either[AppError, AddMoneyResponse]]
}

object SavingsGoalClient {

  def impl[F[_]: Sync](
      starlingSavingsGoalsApi: StarlingSavingsGoalsApi[F]
  ): SavingsGoalClient[F] = new SavingsGoalClient[F] {

    /** Step 1: Read the goal file and extract UUID if present Returns:
      *   - Right(Some(uuid)) if file exists and contains valid UUID
      *   - Right(None) if file is empty or doesn't exist
      *   - Left(error) if file exists but contains invalid data
      */
    override def readGoalFromFile(
        config: AppConfig
    ): F[Either[AppError, Option[UUID]]] = ???
      // try to read file at config.goalFilePath
      // if there is no file or it's empty - return None
      // if there is a value in the file, parse it and try to convert it into a UUID
      // if it is a UUID, then return Some(UUID)
      // if it is not a valid UUID, return InvalidStoredSavingsGoalUuid (of type AppError)

    /** Step 2: Validate that a goal UUID exists in the user's Starling account
      * Returns:
      *   - Right(savingsGoal) if the goal exists and is valid
      *   - Left(error) if goal doesn't exist or API call fails
      */
    override def validateGoal(
        goalUuid: UUID,
        accountUid: UUID
    ): F[Either[AppError, SavingsGoal]] = {
      // call starlingSavingsGoalsApi.getSavingsGoals(accountUid: String): F[SavingsGoalsResponse]
      // case class SavingsGoalsResponse( savingsGoalList: List[SavingsGoal] )
      // case class SavingsGoal(
      //    savingsGoalUid: UUID,
      //    name: String,
      //    target: Option[CurrencyAndAmount],
      //    totalSaved: CurrencyAndAmount,
      //    savedPercentage: Option[Int],
      //    state: String
      // )
      // if the goalUuid matches one in the list, then return that Savings Goal
      // if there is no match OR API call fails, then return InvalidStoredSavingsGoalUuid
      starlingSavingsGoalsApi
        .getSavingsGoals(accountUid.toString)
        .attempt
        .map {
          case Right(response) =>
            response.savingsGoalList
              .find(_.savingsGoalUid == goalUuid)
              .toRight(InvalidStoredSavingsGoalUuid(goalUuid.toString))

          case Left(error) =>
            Left(GenericError(error.getMessage))
        }
    }

    /** Step 3: Creates a savings goal, persists its UUID to the goal file, and
      * returns the created SavingsGoal. Returns:
      *   - Right(savingsGoal) if goal created successfully and UUID saved to
      *     file
      *   - Left(error) if creation fails or file write fails
      */

      //TODO file writer reusable helper method? Would that make it easier?
    // call starlingSavingsGoalsApi.createSavingsGoal
    // (accountUid: String, request: CreateSavingsGoalRequest): F[CreateSavingsGoalResponse]
    // case class CreateSavingsGoalRequest(
    //    name: String,
    //    currency: String,
    //    target: Option[CurrencyAndAmount] = None,
    //    base64EncodedPhoto: Option[String] = None
    // )
    // not sure what the name should be?!
    // currency should be GBP

    // if successful here is the response
    // case class CreateSavingsGoalResponse(
    //    savingsGoalUid: UUID,
    //    success: Boolean
    // )
    // if API call fails, then return GenericError
    // write the UUID to the config file at config.goalFilePath
    // if file write fails, return - FileWriteFailure (AppError)
    override def createAndPersistGoal(
        config: AppConfig,
        accountUid: UUID
    ): F[Either[AppError, UUID]] = ???

    override def transferToSavingsGoal(
        config: AppConfig,
        accountUid: UUID,
        goal: UUID,
        amountMinorUnits: Long
    ): F[Either[AppError, AddMoneyResponse]] = {
      val transferUid = UUID.randomUUID()
      val request = AddMoneyRequest(
        amount = CurrencyAndAmount(
          currency = "GBP",
          minorUnits = amountMinorUnits
        ),
        reference = Some("Round-up transfer")
      )

      starlingSavingsGoalsApi.addMoney(
        accountUid = accountUid.toString,
        savingsGoalUid = goal.toString,
        transferUid = transferUid.toString,
        request = request
      ).attempt.map {
        case Right(addMoneyResponse) => Right(addMoneyResponse)
        case Left(throwable) => Left(GenericError(s"Failed to transfer to savings goal: ${throwable.getMessage}"))
      }
    }
  }
}
