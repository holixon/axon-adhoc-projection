package io.holixon.axon.projection.adhoc.model

import io.holixon.axon.projection.adhoc.ModelRepository
import org.axonframework.common.caching.NoCache
import org.axonframework.eventhandling.SequenceNumber
import org.axonframework.eventhandling.Timestamp
import org.axonframework.eventsourcing.eventstore.EventStore
import org.axonframework.messaging.annotation.MessageHandler
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

data class CurrentBalanceModel(
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

  @MessageHandler
  fun on(evt: MoneyDepositedEvent, @Timestamp messageTimestamp: Instant, @SequenceNumber version: Long): CurrentBalanceModel = copy(
    currentBalanceInEuroCent = this.currentBalanceInEuroCent + evt.amountInEuroCent,
    lastModification = messageTimestamp,
    version = version,
  )

  @MessageHandler
  fun on(evt: MoneyWithdrawnEvent, @Timestamp messageTimestamp: Instant, @SequenceNumber version: Long): CurrentBalanceModel = copy(
    currentBalanceInEuroCent = this.currentBalanceInEuroCent - evt.amountInEuroCent,
    lastModification = messageTimestamp,
    version = version,
  )
}

@Component
class CurrentBalanceModelRepository(eventStore: EventStore) :
  ModelRepository<CurrentBalanceModel>(eventStore, CurrentBalanceModel::class.java, NoCache.INSTANCE)
