package io.holixon.axon.projection.adhoc.model.numwithdrawals

import io.holixon.axon.projection.adhoc.ModelRepositoryConfig
import io.holixon.axon.projection.adhoc.UpdatingModelRepository
import io.holixon.axon.projection.adhoc.model.BankAccountCreatedEvent
import io.holixon.axon.projection.adhoc.model.MoneyWithdrawnEvent
import mu.KLogging
import org.axonframework.eventhandling.SequenceNumber
import org.axonframework.eventhandling.Timestamp
import org.axonframework.eventsourcing.eventstore.EventStore
import org.axonframework.messaging.annotation.MessageHandler
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

data class NumWithdrawalsModel(
  val bankAccountId: UUID,
  val numWithdrawals: Int,
  val lastModification: Instant,
  val version: Long
) {

  @MessageHandler
  constructor(evt: BankAccountCreatedEvent, @Timestamp messageTimestamp: Instant, @SequenceNumber version: Long) : this(
    bankAccountId = evt.bankAccountId,
    numWithdrawals = 0,
    lastModification = messageTimestamp,
    version = version,
  )

  @MessageHandler
  fun on(evt: MoneyWithdrawnEvent, @Timestamp messageTimestamp: Instant, @SequenceNumber version: Long) = copy(
    numWithdrawals = numWithdrawals+1,
    lastModification = messageTimestamp,
    version = version,
  )
}

//@Component
class NumWithdrawalsModelRepository(eventStore: EventStore) :
  UpdatingModelRepository<NumWithdrawalsModel>(
    eventStore,
    NumWithdrawalsModel::class.java,
    ModelRepositoryConfig(forceCacheInsert = true)
  )
