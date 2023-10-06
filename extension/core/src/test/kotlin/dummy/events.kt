package io.holixon.axon.selectivereplay.dummy

import java.util.UUID

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
