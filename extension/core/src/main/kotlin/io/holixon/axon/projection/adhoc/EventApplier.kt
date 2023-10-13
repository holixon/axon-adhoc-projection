package io.holixon.axon.projection.adhoc

import org.axonframework.eventhandling.DomainEventMessage

class EventApplier<T>(
  private val modelInspector: ModelInspector<T>
) {

  fun <E> applyEvent(model: T, event: DomainEventMessage<E>) : T {
    return modelInspector.findEventHandler(event.payloadType)
      ?.handle(event, model) as T // assured in inspector that the only return type allowed return type is model class
      ?: model
  }
}
