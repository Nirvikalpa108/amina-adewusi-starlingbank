package com.adewusi.roundup.starlingapis

import cats.effect.IO
import com.adewusi.roundup.RoundupRoutes
import com.adewusi.roundup.model.{Account, AccountsResponse}
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.implicits._

import java.util.UUID

class StarlingAccountsApiRoutesSpec extends CatsEffectSuite {

  private val sampleAccountsResp: AccountsResponse = AccountsResponse(
    accounts = List(
      Account(
        accountUid = UUID.fromString("f2ca1bb6-c7e4-4e77-b0c4-e63c5c5e5e5e"),
        accountType = "PRIMARY",
        defaultCategory = "b2ca1bb6-c7e4-4e77-b0c4-e63c5c5e5e5e",
        currency = "GBP",
        createdAt = "2023-01-01T00:00:00.000Z",
        name = "Personal"
      )
    )
  )

  test("GET /accounts returns status code 200") {
    assertIO(retAccountsSuccess.map(_.status), Status.Ok)
  }

  test("GET /accounts returns AccountsResponse JSON") {
    assertIO(retAccountsSuccess.flatMap(_.as[AccountsResponse]), sampleAccountsResp)
  }

  test("GET /accounts returns 500 Internal Server Error when service fails") {
    assertIO(retAccountsError.map(_.status), Status.InternalServerError)
  }

  test("GET /accounts returns 'Internal error' body when service fails") {
    assertIO(retAccountsError.flatMap(_.as[String]).map(_.contains("Internal error")), true)
  }

  private[this] val retAccountsSuccess: IO[Response[IO]] = {
    val req = Request[IO](Method.GET, uri"/accounts")
    val accounts = new StarlingAccountsApi[IO] {
      override def getAccounts(): IO[AccountsResponse] = IO.pure(sampleAccountsResp)
    }
    RoundupRoutes.accountsRoutes[IO](accounts).orNotFound(req)
  }

  private[this] val retAccountsError: IO[Response[IO]] = {
    val req = Request[IO](Method.GET, uri"/accounts")
    val accounts = new StarlingAccountsApi[IO] {
      override def getAccounts(): IO[AccountsResponse] =
        IO.raiseError(new RuntimeException("failure"))
    }
    RoundupRoutes.accountsRoutes[IO](accounts).orNotFound(req)
  }
}