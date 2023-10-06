package io.holixon.axon.selectivereplay

import org.axonframework.messaging.annotation.AnnotatedHandlerInspector
import org.axonframework.messaging.annotation.ClasspathHandlerDefinition
import org.axonframework.messaging.annotation.ClasspathParameterResolverFactory
import org.axonframework.messaging.annotation.HandlerDefinition
import org.axonframework.messaging.annotation.MessageHandlingMember
import org.axonframework.messaging.annotation.ParameterResolverFactory
import java.lang.reflect.Constructor
import java.util.function.Consumer

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
      .filter { method -> method.returnType.equals(inspectedType) || method.returnType.equals(Void::class.java) }
      .forEach { method ->
        handlerDefinition.createHandler(inspectedType, method, parameterResolverFactory)
          .ifPresent {
            if (!(method.returnType.equals(inspectedType) || method.returnType.equals(Void::class.java)))
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
