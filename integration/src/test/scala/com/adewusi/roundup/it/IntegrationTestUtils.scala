package com.adewusi.roundup.it

import cats.effect.{IO, Ref, Resource}
import com.adewusi.roundup.Config
import com.adewusi.roundup.clients.AccountClient
import com.adewusi.roundup.domain.AccountSelector
import com.adewusi.roundup.model.{Account, AppConfig, TransferRecord}
import com.adewusi.roundup.starlingapis.StarlingAccountsApi
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.headers.Authorization
import org.http4s.{AuthScheme, Credentials, Method, Request, Uri}

import java.util.UUID

final case class AppContext(
    goalRef: Ref[IO, Option[UUID]],
    transferRef: Ref[IO, Set[TransferRecord]],
    config: AppConfig,
    accountUuid: UUID
)

trait IntegrationTestUtils {
  def loadConfig: IO[AppConfig] = Config.load

  def buildClient: Resource[IO, Client[IO]] =
    EmberClientBuilder.default[IO].build

  def createStarlingAccountsApi(client: Client[IO]): StarlingAccountsApi[IO] =
    StarlingAccountsApi.impl[IO](client)

  def createAccountClient(config: AppConfig)(implicit
      starlingAccountsApi: StarlingAccountsApi[IO]
  ): AccountClient[IO] = AccountClient.impl[IO](config)

  def fetchAccountUuid(accountClient: AccountClient[IO]): IO[UUID] = {
    accountClient.fetchAccounts.flatMap {
      case Right(accounts: List[Account]) =>
        AccountSelector.impl.getCorrectAccount(accounts) match {
          case Right(account) => IO.pure(account.accountUid)
          case Left(error) =>
            IO.raiseError(new Exception(s"Failed to select account: $error"))
        }
      case Left(error) =>
        IO.raiseError(new Exception(s"Failed to fetch accounts: $error"))
    }
  }

  def createGoalRef(initialState: Option[UUID]): IO[Ref[IO, Option[UUID]]] =
    Ref.of[IO, Option[UUID]](initialState)

  def createTransferRef(
      initialState: Set[TransferRecord]
  ): IO[Ref[IO, Set[TransferRecord]]] =
    Ref.of[IO, Set[TransferRecord]](initialState)

  def setupResources: Resource[IO, Client[IO]] = buildClient

  def setupApp(
      client: Client[IO],
      goalInitialState: Option[UUID],
      transferRepoInitialState: Set[TransferRecord]
  ): IO[AppContext] = for {
    config <- loadConfig
    starlingAccountsApi = createStarlingAccountsApi(client)
    accountClient = createAccountClient(config)(starlingAccountsApi)
    accountUuid <- fetchAccountUuid(accountClient)
    goalRef <- createGoalRef(goalInitialState)
    transferRef <- createTransferRef(transferRepoInitialState)
  } yield AppContext(goalRef, transferRef, config, accountUuid)

  // Helper to delete savings goal via Starling API
  def deleteSavingsGoal(
      baseUrl: Uri,
      client: Client[IO],
      accessToken: String,
      accountId: UUID,
      goalId: UUID
  ): IO[Unit] = {
    val deleteUri = Uri.unsafeFromString(
      s"$baseUrl/api/v2/account/$accountId/savings-goals/$goalId"
    )
    val request = Request[IO](method = Method.DELETE, uri = deleteUri)
      .putHeaders(
        Authorization(Credentials.Token(AuthScheme.Bearer, accessToken))
      )

    IO.println(s"Deleting savings goal at $deleteUri") *>
      client.run(request).use { resp =>
        if (resp.status.isSuccess)
          IO.println(s"Successfully deleted savings goal: $goalId")
        else
          IO.raiseError(
            new Exception(s"Failed to delete savings goal: ${resp.status}")
          )
      }
  }

}
