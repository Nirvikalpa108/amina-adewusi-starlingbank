package com.adewusi.roundup.domain

import com.adewusi.roundup.model._

trait AccountSelector {
  def getCorrectAccount(accounts: List[Account]): Either[AppError, Account]
}

object AccountSelector {
  def impl: AccountSelector = new AccountSelector {

    /** Selects the correct account from a list of accounts.
      *
      * The method searches for the first account that has:
      *   - currency equal to "GBP"
      *   - accountType equal to "PRIMARY"
      *
      * It assumes that there will only be one PRIMARY GBP account.
      *
      * @param accounts
      *   the list of accounts to search through
      * @return
      *   Right(account) if a matching account is found; Left(NoAccount) if none
      *   match
      */
    def getCorrectAccount(
        accounts: List[Account]
    ): Either[AppError, Account] = {
      accounts
        .find(account =>
          account.currency == "GBP" && account.accountType == "PRIMARY"
        )
        .toRight(NoAccount)
    }
  }
}
