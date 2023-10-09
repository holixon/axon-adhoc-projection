package io.holixon.selectivereplay

import java.lang.reflect.Method

class DuplicateHandlerException(
  clazz: Class<*>,
  payloadClazz: Class<*>
) :
  Exception("At least two eventHandlers in the annotated class $clazz are capable of handling a payload of type $payloadClazz. This can't be handled")

class IllegalReturnTypeException(
  clazz: Class<*>,
  method: Method,
) :
  Exception("Method $method of class $clazz has disallowed return type ${method.returnType}. Only void or $clazz is allowed here")

class NoEventHandlersFoundException(
  clazz: Class<*>,
) :
  Exception("Class $clazz has no handler defined.")
