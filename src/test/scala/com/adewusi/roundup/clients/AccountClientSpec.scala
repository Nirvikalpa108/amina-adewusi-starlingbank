package com.adewusi.roundup.clients

import cats.effect.IO
import com.adewusi.roundup.model._
import com.adewusi.roundup.starlingapis.StarlingAccountsApi
import munit.CatsEffectSuite

import java.util.UUID

class AccountClientSpec extends CatsEffectSuite {

  // Test data
  val account1 = Account(
    accountUid = UUID.randomUUID(),
    accountType = "PRIMARY",
    defaultCategory = UUID.randomUUID(),
    currency = "GBP",
    createdAt = "2023-01-01T00:00:00.000Z",
    name = "Personal"
  )

  val account2 = account1.copy(
    accountUid = UUID.randomUUID(),
    accountType = "SAVINGS",
    currency = "EUR",
    name = "Euro Account"
  )

  def apiSuccess(accounts: List[Account]) = new StarlingAccountsApi[IO] {
    override def getAccounts(): IO[AccountsResponse] = IO.pure(AccountsResponse(accounts))
  }

  def apiFailure(error: Throwable) = new StarlingAccountsApi[IO] {
    override def getAccounts(): IO[AccountsResponse] = IO.raiseError(error)
  }

  test("fetchAccounts returns all accounts when API succeeds with multiple accounts") {
    implicit val starlingAccountsApi: StarlingAccountsApi[IO] = apiSuccess(List(account1, account2))
    val client = AccountClient.impl[IO]
    assertIO(client.fetchAccounts, Right(List(account1, account2)))
  }

  test("fetchAccounts returns an empty list when API succeeds with no accounts") {
    implicit val starlingAccountsApi: StarlingAccountsApi[IO] = apiSuccess(Nil)
    val client = AccountClient.impl[IO]
    assertIO(client.fetchAccounts, Right(Nil))
  }

  test("fetchAccounts returns an error when API fails with RuntimeException") {
    implicit val starlingAccountsApi: StarlingAccountsApi[IO] = apiFailure(new RuntimeException("network down"))
    val client = AccountClient.impl[IO]

    client.fetchAccounts.map { result =>
      assert(result.isLeft)
      val err = result.left.toOption.get
      assert(err.isInstanceOf[GenericError])
      assert(err.asInstanceOf[GenericError].reason.contains("Failed to fetch accounts"))
      assert(err.asInstanceOf[GenericError].reason.contains("network down"))
    }
  }

  test("fetchAccounts returns an error when API fails with IOException") {
    implicit val starlingAccountsApi: StarlingAccountsApi[IO] = apiFailure(new java.io.IOException("connection timeout"))
    val client = AccountClient.impl[IO]

    client.fetchAccounts.map { result =>
      assert(result.isLeft)
      val err = result.left.toOption.get
      assert(err.isInstanceOf[GenericError])
      assert(err.asInstanceOf[GenericError].reason.contains("connection timeout"))
    }
  }

  test("fetchAccounts handles exception without message gracefully") {
    implicit val starlingAccountsApi: StarlingAccountsApi[IO] = apiFailure(new RuntimeException(null: String))
    val client = AccountClient.impl[IO]

    client.fetchAccounts.map { result =>
      assert(result.isLeft)
      val err = result.left.toOption.get
      assert(err.isInstanceOf[GenericError])
      assert(err.asInstanceOf[GenericError].reason.contains("Failed to fetch accounts"))
    }
  }
}
