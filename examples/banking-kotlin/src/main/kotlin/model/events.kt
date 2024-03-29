package io.holixon.axon.projection.adhoc.example.model

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

data class OwnerChangedEvent(
  val bankAccountId: UUID,
  val owner: String
)
