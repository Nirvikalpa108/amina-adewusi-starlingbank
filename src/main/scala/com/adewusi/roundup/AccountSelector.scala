package com.adewusi.roundup

import com.adewusi.roundup.model._

trait AccountSelector {
  def getCorrectAccount(accounts: List[Account]): Either[AppError, Account]
}

object AccountSelector {
  def impl: AccountSelector = new AccountSelector {
    def getCorrectAccount(accounts: List[Account]): Either[AppError, Account] = {
      accounts.find(account =>
        account.currency == "GBP" && account.accountType == "PRIMARY"
      ) match {
        case Some(account) => Right(account)
        case None => Left(NoAccount)
      }
    }
  }
}