package io.holixon.axon.selectivereplay

import org.axonframework.eventhandling.DomainEventMessage
import java.lang.reflect.Method

class EventHandler<T>(
  private val method : Method,
  private val clazz: Class<T>
) {

  fun<E> execute(model: T, eventMessage: DomainEventMessage<E>) : T {
    return if (method.returnType == clazz)
      method.invoke(model, eventMessage.payload) as T
    else {
      method.invoke(model, eventMessage.payload)
      model
    }
  }
}
