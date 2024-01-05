package io.holixon.axon.projection.adhoc.dummy

import java.util.*

interface BankAccountEvent {
  val bankAccountId: UUID
}

data class BankAccountCreatedEvent(
  override val bankAccountId: UUID,
  val owner: String,
) : BankAccountEvent

data class MoneyWithdrawnEvent(
  override val bankAccountId: UUID,
  val amountInEuroCent: Int
) : BankAccountEvent

data class MoneyDepositedEvent(
  override val bankAccountId: UUID,
  val amountInEuroCent: Int
) : BankAccountEvent

data class OwnerChangedEvent(
  override val bankAccountId: UUID,
  val owner: String
) : BankAccountEvent
