package io.holixon.axon.projection.adhoc

import org.axonframework.eventhandling.DomainEventMessage
import org.axonframework.eventsourcing.eventstore.DomainEventStream

/**
 * The model factory is responsible for instantiating the model.
 */
class ModelFactory<T>(
  private val modelInspector: ModelInspector<T>
) {

  /**
   * Takes the event stream and tries to construct a model instance. Either by taking the first event from the stream and using a
   * constructor capable of accepting this event or by using a default constructor. Then the first event is used for construction,
   * that event is marked as consumed in the stream.
   *
   * @param events the event stream
   * @return an instance of the model class
   * @throws NoSuchMethodException when no usable constructor is found
   */
  fun createInstanceFromStream(events: DomainEventStream): T {
    val firstEvent = events.peek()

    return if (canCreateFromEvent(firstEvent.payloadType)) {
      val instance = createInstance(firstEvent)
      events.next() // mark first event as consumed
      instance
    } else {
      // do not mark first event as consumed
      createInstance()
    }
  }

  private fun createInstance(message: DomainEventMessage<*>): T {
    modelInspector.findConstructor(message.payloadType)?.let {
      return it.handle(message, null) as T
    }

    throw NoSuchMethodException("No suitable constructor found for payload ${message.payloadType}")
  }

  private fun createInstance(): T {
    return modelInspector.getDefaultConstructor().newInstance()
  }

  private fun canCreateFromEvent(eventClazz: Class<*>): Boolean =
    modelInspector.findConstructor(eventClazz) != null
}
