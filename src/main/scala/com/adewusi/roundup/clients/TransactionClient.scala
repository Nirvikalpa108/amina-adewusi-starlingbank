package com.adewusi.roundup.clients

import cats.effect.Concurrent
import cats.syntax.all._
import com.adewusi.roundup.model._
import com.adewusi.roundup.starlingapis.StarlingTransactionApi

import java.time.{LocalDate, ZoneOffset, ZonedDateTime}

trait TransactionClient[F[_]] {
  def fetchTransactions(
      account: Account,
      startDate: LocalDate
  ): F[Either[AppError, List[TransactionFeedItem]]]
}

object TransactionClient {
  def impl[F[_]: Concurrent](
      implicit starlingTransactionApi: StarlingTransactionApi[F]
  ): TransactionClient[F] =
    new TransactionClient[F] {
      def fetchTransactions(
          account: Account,
          startDate: LocalDate
      ): F[Either[AppError, List[TransactionFeedItem]]] = {
        val startUtc: ZonedDateTime = startDate.atStartOfDay(ZoneOffset.UTC)
        val endUtc: ZonedDateTime = startUtc.plusDays(7)
        starlingTransactionApi
          .getSettledTransactionsBetween(account.accountUid, startUtc, endUtc)
          .attempt
          .map {
            case Right(response) => Right(response.feedItems)
            case Left(error) =>
              Left(
                GenericError(
                  s"Failed to fetch transactions: ${error.getMessage}"
                )
              )
          }
      }
    }
}
