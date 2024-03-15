package io.holixon.axon.projection.adhoc

import io.holixon.axon.projection.adhoc.AdhocEventMessageHandler.Companion.PROCESSING_GROUP
import mu.KLogging
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.DomainEventMessage
import org.axonframework.eventhandling.EventMessage
import org.axonframework.eventhandling.EventMessageHandler

/**
 * Special EventMessageHandler to react to all DomainEventMessages and manually check if we have a matching model class method.
 */
@ProcessingGroup(PROCESSING_GROUP)
class AdhocEventMessageHandler(
  private val updatingModelRepositories: List<UpdatingModelRepository<*>>
) : EventMessageHandler {

  companion object : KLogging() {
    const val PROCESSING_GROUP = "adhoc-event-message-handler"
  }

  override fun handle(event: EventMessage<*>?) {
    if (event !is DomainEventMessage)
      return

    logger.trace { "Received event ${event.aggregateIdentifier}/${event.sequenceNumber} of type ${event.payloadType.simpleName}" }

    val failed = mutableListOf<UpdatingModelRepository<*>>()
    updatingModelRepositories.filter { it.canHandleMessage(event) }
      .forEach {
        try {
          it.on(event)
        } catch (e: java.lang.Exception) {
          logger.error(e) { "Error while applying event ${event.payloadType.simpleName} with seqNo ${event.sequenceNumber} of aggregateId ${event.aggregateIdentifier}" }
          failed.add(it)
        }
      }

    if (failed.isNotEmpty()) {
      failed.map { it.javaClass.simpleName }.let { throw ProcessingFailureException(it) }
    }
  }

  override fun canHandle(message: EventMessage<*>?): Boolean {
    if (message !is DomainEventMessage)
      return false

    return updatingModelRepositories.any { it.canHandleMessage(message) }
  }

  override fun prepareReset() {
    updatingModelRepositories.forEach { it.resetCache() }
  }
}

/**
 * Indicates that at least one of the registered repositories failed to process the event.

 * @param failedRepositories list of the failed repository class names
 */
class ProcessingFailureException(failedRepositories: List<String>) :
  Exception("Failed to apply event to following UpdatingModelRepositories: $failedRepositories")
