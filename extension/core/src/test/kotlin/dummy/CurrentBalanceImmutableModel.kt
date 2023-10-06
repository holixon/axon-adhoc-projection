package io.holixon.axon.selectivereplay.dummy

import io.holixon.axon.selectivereplay.ModelRepository
import org.axonframework.eventsourcing.eventstore.EventStore
import java.util.*

data class CurrentBalanceImmutableModel(
  val bankAccountId: UUID,
  val currentBalanceInEuroCent: Int
) {

  constructor(evt: BankAccountCreatedEvent) : this(
    bankAccountId = evt.bankAccountId,
    currentBalanceInEuroCent = 0
  )

  fun on(evt: MoneyDepositedEvent): CurrentBalanceImmutableModel = copy(
    currentBalanceInEuroCent = this.currentBalanceInEuroCent + evt.amountInEuroCent
  )

  fun on(evt: MoneyWithdrawnEvent): CurrentBalanceImmutableModel = copy(
    currentBalanceInEuroCent = this.currentBalanceInEuroCent - evt.amountInEuroCent
  )
}

class CurrentBalanceImmutableModelRepository(eventStore: EventStore) :
  ModelRepository<CurrentBalanceImmutableModel>(eventStore, CurrentBalanceImmutableModel::class.java)
