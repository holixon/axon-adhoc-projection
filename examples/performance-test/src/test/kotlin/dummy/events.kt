package io.holixon.selectivereplay.dummy

import java.util.*

data class BankAccountCreatedEvent(
  val bankAccountId: UUID,
  val owner: String,
)

data class MoneyWithdrawnEvent(
  val bankAccountId: UUID,
  val amountInEuroCent: Int
)

data class MoneyDepositedEvent(
  val bankAccountId: UUID,
  val amountInEuroCent: Int
)
