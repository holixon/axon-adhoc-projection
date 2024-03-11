package io.holixon.axon.projection.adhoc.example.model

import io.holixon.axon.projection.adhoc.ModelRepository
import io.holixon.axon.projection.adhoc.ModelRepositoryConfig
import io.holixon.axon.projection.adhoc.UpdatingModelRepository
import mu.KLogging
import org.axonframework.eventhandling.SequenceNumber
import org.axonframework.eventhandling.Timestamp
import org.axonframework.eventsourcing.eventstore.EventStore
import org.axonframework.messaging.annotation.MessageHandler
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

data class CurrentOwnerModel(
  val bankAccountId: UUID,
  val owner: String
) {

  @MessageHandler
  constructor(evt: BankAccountCreatedEvent) : this(
    bankAccountId = evt.bankAccountId,
    owner = evt.owner
  )

  @MessageHandler
  fun on(evt: OwnerChangedEvent): CurrentOwnerModel = copy(
    owner = evt.owner
  )
}

@Component
class CurrentOwnerModelRepository(eventStore: EventStore) :
  UpdatingModelRepository<CurrentOwnerModel>(
    eventStore,
    CurrentOwnerModel::class.java,
    ModelRepositoryConfig.defaults()
      .withCacheRefreshTime(10000) // 10 seconds
  )
