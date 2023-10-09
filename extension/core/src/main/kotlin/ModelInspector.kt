package io.holixon.selectivereplay

import org.axonframework.messaging.annotation.*
import java.lang.reflect.Constructor

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


  fun getDefaultConstructor(): Constructor<T> {
    return inspectedType.getConstructor()
  }

  fun <E> findConstructor(eventClass: Class<E>): MessageHandlingMember<T>? {
    return constructors[eventClass]
  }

  fun <E> findEventHandler(eventClazz: Class<E>): MessageHandlingMember<T>? {
    return methods[eventClazz]
  }

  internal fun inspectForMethods(parameterResolverFactory: ParameterResolverFactory, handlerDefinition: HandlerDefinition) {
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

  internal fun inspectForConstructors(parameterResolverFactory: ParameterResolverFactory, handlerDefinition: HandlerDefinition) {
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
