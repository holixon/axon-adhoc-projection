package io.holixon.axon.projection.adhoc

import org.axonframework.messaging.annotation.*
import java.lang.reflect.Constructor

/**
 * Inspects the model class for constructors and methods annotated with <code>@MessageHandler</code>
 *
 * @param inspectedType the model class to inspect
 */
class ModelInspector<T>(
  val inspectedType: Class<T>
) {
  val constructors = mutableMapOf<Class<*>, MessageHandlingMember<T>>()
  val methods = mutableMapOf<Class<*>, MessageHandlingMember<T>>()

  init {
    val parameterResolverFactory = ClasspathParameterResolverFactory.forClass(inspectedType)
    val handlerDefinition = ClasspathHandlerDefinition.forClass(inspectedType)

    inspectForMethods(parameterResolverFactory, handlerDefinition)
    inspectForConstructors(parameterResolverFactory, handlerDefinition)

    if (constructors.isEmpty() && methods.isEmpty()) {
      throw NoEventHandlersFoundException(inspectedType)
    }
  }

  /**
   * returns the default constructor.
   */
  fun getDefaultConstructor(): Constructor<T> {
    return inspectedType.getConstructor()
  }

  /**
   * Looks for a constructor able to handle the given payload.
   *
   * @param eventClass the class of the event payload
   * @return messageHandlingMember or <code>null</code> if none found
   */
  fun <E> findConstructor(eventClass: Class<E>): MessageHandlingMember<T>? {
    return constructors[eventClass]
  }

  /**
   * Looks for a method able to handle the given payload.
   *
   * @param eventClass the class of the event payload
   * @return messageHandlingMember or <code>null</code> if none found
   */
  fun <E> findEventHandler(eventClass: Class<E>): MessageHandlingMember<T>? {
    return methods[eventClass]
  }

  private fun inspectForMethods(parameterResolverFactory: ParameterResolverFactory, handlerDefinition: HandlerDefinition) {
    inspectedType.getDeclaredMethods()
      .forEach { method ->
        handlerDefinition.createHandler(inspectedType, method, parameterResolverFactory)
          .ifPresent {
            if (!(method.returnType.equals(inspectedType) || method.returnType.equals(Void.TYPE)))
              throw IllegalReturnTypeException(inspectedType, method)
            if (methods.containsKey(it.payloadType())) {
              throw DuplicateHandlerException(inspectedType, it.payloadType())
            }

            methods[it.payloadType()] = it as MessageHandlingMember<T>
          }
      }
  }

  private fun inspectForConstructors(parameterResolverFactory: ParameterResolverFactory, handlerDefinition: HandlerDefinition) {
    inspectedType.getDeclaredConstructors().forEach { constructor ->
      handlerDefinition.createHandler(inspectedType, constructor, parameterResolverFactory)
        .ifPresent {
          if (constructors.containsKey(it.payloadType())) {
            throw DuplicateHandlerException(inspectedType, it.payloadType())
          }

          constructors[it.payloadType()] = it as MessageHandlingMember<T>
        }
    }
  }
}
