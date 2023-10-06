package io.holixon.axon.selectivereplay

import org.axonframework.eventhandling.DomainEventMessage
import org.axonframework.eventsourcing.eventstore.DomainEventStream

class ModelFactory<T>(
  private val modelInspector: ModelInspector<T>
) {

  fun createInstanceFromStream(events: DomainEventStream): T {
    val firstEvent = events.peek()

    if (canCreateFromEvent(firstEvent.payloadType)) {
      val instance = createInstance(firstEvent)
      events.next() // mark first event as consumed
      return instance
    } else {
      // do not mark first event as consumed
      return createInstance()
    }
  }

  fun createInstance(message: DomainEventMessage<*>): T {
    modelInspector.findConstructor(message.payloadType)?.let {
      return it.handle(message, null) as T
    }

    throw NoSuchMethodException("No suitable constructor found for payload ${message.payloadType}")
  }

  fun createInstance(): T {
    return modelInspector.getDefaultConstructor().newInstance()
  }

  internal fun canCreateFromEvent(eventClazz: Class<*>): Boolean =
    modelInspector.findConstructor(eventClazz) != null
}
