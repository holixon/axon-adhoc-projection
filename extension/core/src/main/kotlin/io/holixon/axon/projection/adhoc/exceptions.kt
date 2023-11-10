package io.holixon.axon.projection.adhoc

import java.lang.reflect.Method

/**
 * There are two handlers annotated with <code>@MessageHandler</code> which can handle the same payload domain event. Event handling methods must be unique.
 */
class DuplicateHandlerException(
  clazz: Class<*>,
  payloadClazz: Class<*>
) :
  Exception("At least two eventHandlers in the annotated class $clazz are capable of handling a payload of type $payloadClazz. This can't be handled")

/**
 * Illegal return type. A method annotated with <code>@MessageHandler</code> in model classes can only return <code>void</code> or the model class type itself.
 */
class IllegalReturnTypeException(
  clazz: Class<*>,
  method: Method,
) :
  Exception("Method $method of class $clazz has disallowed return type ${method.returnType}. Only void or $clazz is allowed here")


/**
 * The given model class has no methods or constructors annotated with <code>@MessageHandler</code>.
 */
class NoEventHandlersFoundException(
  clazz: Class<*>,
) :
  Exception("Class $clazz has no handler defined.")
