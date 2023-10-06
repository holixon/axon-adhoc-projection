package io.holixon.axon.selectivereplay.dummy

import io.holixon.axon.selectivereplay.ModelRepository
import org.axonframework.common.caching.WeakReferenceCache
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.SequenceNumber
import org.axonframework.eventhandling.Timestamp
import org.axonframework.eventsourcing.eventstore.EventStore
import org.axonframework.messaging.annotation.MessageHandler
import java.time.Instant
import java.util.*

data class CurrentBalanceImmutableModel(
  val bankAccountId: UUID,
  val currentBalanceInEuroCent: Int,
  val lastModification: Instant,
  val version: Long
) {

  @MessageHandler
  constructor(evt: BankAccountCreatedEvent, @Timestamp messageTimestamp: Instant, @SequenceNumber version: Long) : this(
    bankAccountId = evt.bankAccountId,
    currentBalanceInEuroCent = 0,
    lastModification = messageTimestamp,
    version = version,
  )

  @EventHandler
  fun on(evt: MoneyDepositedEvent, @Timestamp messageTimestamp: Instant, @SequenceNumber version: Long): CurrentBalanceImmutableModel = copy(
    currentBalanceInEuroCent = this.currentBalanceInEuroCent + evt.amountInEuroCent,
    lastModification = messageTimestamp,
    version = version,
  )

  @EventHandler
  fun on(evt: MoneyWithdrawnEvent, @Timestamp messageTimestamp: Instant, @SequenceNumber version: Long): CurrentBalanceImmutableModel = copy(
    currentBalanceInEuroCent = this.currentBalanceInEuroCent - evt.amountInEuroCent,
    lastModification = messageTimestamp,
    version = version,
  )
}

class CurrentBalanceImmutableModelRepository(eventStore: EventStore) :
  ModelRepository<CurrentBalanceImmutableModel>(eventStore, CurrentBalanceImmutableModel::class.java, WeakReferenceCache())
