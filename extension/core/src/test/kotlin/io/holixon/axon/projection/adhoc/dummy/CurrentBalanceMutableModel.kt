package io.holixon.axon.projection.adhoc.dummy

import io.holixon.axon.projection.adhoc.ModelRepository
import org.axonframework.common.caching.WeakReferenceCache
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.SequenceNumber
import org.axonframework.eventhandling.Timestamp
import org.axonframework.eventsourcing.eventstore.EventStore
import java.time.Instant
import java.util.*

class CurrentBalanceMutableModel {
  lateinit var bankAccountId: UUID
  var currentBalanceInEuroCent: Int = 0

  lateinit var lastModification: Instant
  var version: Long = 0

  @EventHandler
  fun on(evt: BankAccountCreatedEvent, @Timestamp messageTimestamp: Instant, @SequenceNumber version: Long) {
    this.bankAccountId = evt.bankAccountId
    this.currentBalanceInEuroCent = 0
    this.lastModification = messageTimestamp
    this.version = version
  }

  @EventHandler
  fun on(evt: MoneyDepositedEvent, @Timestamp messageTimestamp: Instant, @SequenceNumber version: Long) {
    this.currentBalanceInEuroCent += evt.amountInEuroCent
    this.lastModification = messageTimestamp
    this.version = version
  }

  @EventHandler
  fun on(evt: MoneyWithdrawnEvent, @Timestamp messageTimestamp: Instant, @SequenceNumber version: Long) {
    this.currentBalanceInEuroCent -= evt.amountInEuroCent
    this.lastModification = messageTimestamp
    this.version = version
  }
}

class CurrentBalanceMutableModelRepository(eventStore: EventStore) :
  ModelRepository<CurrentBalanceMutableModel>(eventStore, CurrentBalanceMutableModel::class.java, WeakReferenceCache())
