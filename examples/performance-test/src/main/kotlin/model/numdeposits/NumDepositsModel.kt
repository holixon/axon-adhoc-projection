package io.holixon.axon.projection.adhoc.model.numdeposits

import io.holixon.axon.projection.adhoc.ModelRepositoryConfig
import io.holixon.axon.projection.adhoc.UpdatingModelRepository
import io.holixon.axon.projection.adhoc.model.BankAccountCreatedEvent
import io.holixon.axon.projection.adhoc.model.MoneyDepositedEvent
import mu.KLogging
import org.axonframework.eventhandling.SequenceNumber
import org.axonframework.eventhandling.Timestamp
import org.axonframework.eventsourcing.eventstore.EventStore
import org.axonframework.messaging.annotation.MessageHandler
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

data class NumDepositsModel(
  val bankAccountId: UUID,
  val numDeposits: Int,
  val lastModification: Instant,
  val version: Long
) {

  @MessageHandler
  constructor(evt: BankAccountCreatedEvent, @Timestamp messageTimestamp: Instant, @SequenceNumber version: Long) : this(
    bankAccountId = evt.bankAccountId,
    numDeposits = 0,
    lastModification = messageTimestamp,
    version = version,
  )

  @MessageHandler
  fun on(evt: MoneyDepositedEvent, @Timestamp messageTimestamp: Instant, @SequenceNumber version: Long) = copy(
    numDeposits = numDeposits+1,
    lastModification = messageTimestamp,
    version = version,
  )
}

@Component
class NumDepositsModelRepository(eventStore: EventStore) :
  UpdatingModelRepository<NumDepositsModel>(
    eventStore,
    NumDepositsModel::class.java,
    ModelRepositoryConfig(forceCacheInsert = true)
  )
