package io.holixon.axon.projection.adhoc

import org.axonframework.eventhandling.DomainEventMessage

/**
 * The eventApplier is responsible for applying domain events to the current model instance.
 */
class EventApplier<T>(
  private val modelInspector: ModelInspector<T>
) {

  /**
   * Checks if the given event message is relevant for the configured model.
   *
   * @param event the event message
   * @return true if the event message can be handled by the model, otherwise false
   */
  fun <E> isRelevant(event: DomainEventMessage<E>): Boolean =
    modelInspector.findEventHandler(event.payloadType) != null

  /**
   * Tries to apply the given event to the given model instance.
   * <ul>
   *   <li>if no annotated MessageHandler is found for the domainEvent's payload type, the model will be returned untouched. </li>
   *   <li>if an annotated MessageHandler is found the event will be applied using that method. If the return value of the method call is
   *   again a model instance, that instance will be returned (immutable pattern), otherwise the original model instance (mutable pattern)
   *
   * @param model the current model instance
   * @param event the domain event to apply
   */
  fun <E> applyEvent(model: T, event: DomainEventMessage<E>): T {
    return modelInspector.findEventHandler(event.payloadType)
      ?.handle(event, model) as T // assured in inspector that the only return type allowed return type is model class
      ?: model
  }
}
