package io.holixon.axon.projection.adhoc

import mu.KLogging
import org.axonframework.eventhandling.DomainEventMessage
import org.axonframework.eventhandling.EventMessage
import org.axonframework.eventhandling.EventMessageHandler

class AdhocEventMessageHandler: EventMessageHandler {

  private val updatingModelRepositories: MutableList<UpdatingModelRepository<*>> = mutableListOf()

  companion object: KLogging()

  fun addRepository(repository: UpdatingModelRepository<*>) {
    logger.debug { "Adding ${repository.javaClass.simpleName} to list of observed repositories" }
    updatingModelRepositories.add(repository)
  }

  override fun handle(event: EventMessage<*>?) {
    if (event !is DomainEventMessage)
      return

    logger.trace { "Received event ${event.aggregateIdentifier}/${event.sequenceNumber} of type ${event.payloadType.simpleName}" }

    return updatingModelRepositories.filter { it.canHandleMessage(event) }
      .forEach { it.on(event) }
  }

  override fun canHandle(message: EventMessage<*>?): Boolean {
    if (message !is DomainEventMessage)
      return false

    return updatingModelRepositories.any { it.canHandleMessage(message) }
  }
}
