package com.adewusi.roundup.clients

import cats.effect.Concurrent
import cats.syntax.all._
import com.adewusi.roundup.model._
import com.adewusi.roundup.starlingapis.StarlingAccountsApi

trait AccountClient[F[_]] {
  def fetchAccounts: F[Either[AppError, List[Account]]]
}

object AccountClient {
  def impl[F[_]: Concurrent](config: AppConfig)(implicit starlingAccountsApi: StarlingAccountsApi[F]): AccountClient[F] = new AccountClient[F] {
    def fetchAccounts: F[Either[AppError, List[Account]]] = {
      starlingAccountsApi.getAccounts(config.starling.accessToken).attempt.map{
        case Right(response) => Right(response.accounts)
        case Left(error) => Left(GenericError(s"Failed to fetch accounts: ${error.getMessage}"))
      }
    }
  }
}