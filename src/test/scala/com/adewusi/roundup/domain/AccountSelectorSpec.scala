package com.adewusi.roundup.domain

import com.adewusi.roundup.model._
import munit.FunSuite

import java.util.UUID

class AccountSelectorSpec extends FunSuite {

  val gbpPrimaryAccount = Account(
    accountUid = UUID.randomUUID(),
    accountType = "PRIMARY",
    defaultCategory = "cat-uid",
    currency = "GBP",
    createdAt = "2023-01-01T00:00:00.000Z",
    name = "Personal"
  )

  val eurAccount = Account(
    accountUid = UUID.randomUUID(),
    accountType = "PRIMARY",
    defaultCategory = "cat-uid",
    currency = "EUR",
    createdAt = "2023-01-01T00:00:00.000Z",
    name = "Euro Personal"
  )

  val gbpSavingsAccount = Account(
    accountUid = UUID.randomUUID(),
    accountType = "SAVINGS",
    defaultCategory = "cat-uid",
    currency = "GBP",
    createdAt = "2023-01-01T00:00:00.000Z",
    name = "GBP Savings"
  )

  val gbpBusinessAccount = Account(
    accountUid = UUID.randomUUID(),
    accountType = "BUSINESS",
    defaultCategory = "cat-uid",
    currency = "GBP",
    createdAt = "2023-01-01T00:00:00.000Z",
    name = "Business Account"
  )

  val selector = AccountSelector.impl

  test("getCorrectAccount returns GBP PRIMARY account when present") {
    val accounts = List(eurAccount, gbpPrimaryAccount, gbpSavingsAccount)
    val result = selector.getCorrectAccount(accounts)
    assertEquals(result, Right(gbpPrimaryAccount))
  }

  //TODO double check this possibility
  test("getCorrectAccount returns first GBP PRIMARY account when multiple exist") {
    val anotherGbpPrimary = gbpPrimaryAccount.copy(
      accountUid = UUID.randomUUID(),
      name = "Personal 2"
    )
    val accounts = List(eurAccount, gbpPrimaryAccount, anotherGbpPrimary)
    val result = selector.getCorrectAccount(accounts)
    assertEquals(result, Right(gbpPrimaryAccount))
  }

  test("getCorrectAccount returns NoAccount when no GBP PRIMARY account exists") {
    val accounts = List(eurAccount, gbpSavingsAccount, gbpBusinessAccount)
    val result = selector.getCorrectAccount(accounts)
    assertEquals(result, Left(NoAccount))
  }

  test("getCorrectAccount returns NoAccount when accounts list is empty") {
    val result = selector.getCorrectAccount(List.empty)
    assertEquals(result, Left(NoAccount))
  }

  test("getCorrectAccount ignores GBP accounts that are not PRIMARY") {
    val accounts = List(gbpSavingsAccount, gbpBusinessAccount, eurAccount)
    val result = selector.getCorrectAccount(accounts)
    assertEquals(result, Left(NoAccount))
  }

  test("getCorrectAccount ignores PRIMARY accounts that are not GBP") {
    val usdPrimaryAccount = gbpPrimaryAccount.copy(
      accountUid = UUID.randomUUID(),
      currency = "USD",
      name = "USD Personal"
    )
    val accounts = List(eurAccount, usdPrimaryAccount, gbpSavingsAccount)
    val result = selector.getCorrectAccount(accounts)
    assertEquals(result, Left(NoAccount))
  }

  //TODO check this case
  test("getCorrectAccount is case sensitive for currency") {
    val lowercaseGbpAccount = gbpPrimaryAccount.copy(
      accountUid = UUID.randomUUID(),
      currency = "gbp"
    )
    val accounts = List(lowercaseGbpAccount)
    val result = selector.getCorrectAccount(accounts)
    assertEquals(result, Left(NoAccount))
  }

  //TODO check this case
  test("getCorrectAccount is case sensitive for accountType") {
    val lowercasePrimaryAccount = gbpPrimaryAccount.copy(
      accountUid = UUID.randomUUID(),
      accountType = "primary"
    )
    val accounts = List(lowercasePrimaryAccount)
    val result = selector.getCorrectAccount(accounts)
    assertEquals(result, Left(NoAccount))
  }
}