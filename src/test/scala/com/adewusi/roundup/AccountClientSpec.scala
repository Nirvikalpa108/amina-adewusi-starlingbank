package com.adewusi.roundup

import cats.effect.IO
import com.adewusi.roundup.model._
import com.adewusi.roundup.starlingapis.StarlingAccountsApi
import munit.CatsEffectSuite

class AccountClientSpec extends CatsEffectSuite {

  // Test data
  val account1 = Account(
    accountUid = "uid-1",
    accountType = "PRIMARY",
    defaultCategory = "cat-uid-1",
    currency = "GBP",
    createdAt = "2023-01-01T00:00:00.000Z",
    name = "Personal"
  )

  val account2 = account1.copy(
    accountUid = "uid-2",
    accountType = "SAVINGS",
    currency = "EUR",
    name = "Euro Account"
  )

  // Helper Starling API mocks
  def apiSuccess(accounts: List[Account]) = new StarlingAccountsApi[IO] {
    override def getAccounts(): IO[AccountsResponse] = IO.pure(AccountsResponse(accounts))
  }

  def apiFailure(error: Throwable) = new StarlingAccountsApi[IO] {
    override def getAccounts(): IO[AccountsResponse] = IO.raiseError(error)
  }

  test("fetchAccounts returns all accounts when API succeeds with multiple accounts") {
    val client = AccountClient.impl[IO](apiSuccess(List(account1, account2)))
    assertIO(client.fetchAccounts, Right(List(account1, account2)))
  }

  test("fetchAccounts returns an empty list when API succeeds with no accounts") {
    val client = AccountClient.impl[IO](apiSuccess(Nil))
    assertIO(client.fetchAccounts, Right(Nil))
  }

  test("fetchAccounts returns an error when API fails with RuntimeException") {
    val client = AccountClient.impl[IO](apiFailure(new RuntimeException("network down")))

    client.fetchAccounts.map { result =>
      assert(result.isLeft)
      val err = result.left.toOption.get
      assert(err.isInstanceOf[GenericError])
      assert(err.asInstanceOf[GenericError].reason.contains("Failed to fetch accounts"))
      assert(err.asInstanceOf[GenericError].reason.contains("network down"))
    }
  }

  test("fetchAccounts returns an error when API fails with IOException") {
    val client = AccountClient.impl[IO](apiFailure(new java.io.IOException("connection timeout")))

    client.fetchAccounts.map { result =>
      assert(result.isLeft)
      val err = result.left.toOption.get
      assert(err.isInstanceOf[GenericError])
      assert(err.asInstanceOf[GenericError].reason.contains("connection timeout"))
    }
  }

  test("fetchAccounts handles exception without message gracefully") {
    val client = AccountClient.impl[IO](apiFailure(new RuntimeException(null: String)))

    client.fetchAccounts.map { result =>
      assert(result.isLeft)
      val err = result.left.toOption.get
      assert(err.isInstanceOf[GenericError])
      assert(err.asInstanceOf[GenericError].reason.contains("Failed to fetch accounts"))
    }
  }
}