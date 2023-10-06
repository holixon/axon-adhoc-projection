package io.holixon.axon.selectivereplay

import java.lang.Exception
import java.lang.reflect.Method

class NoSuitableConstructorFoundException(
  private val clazz: Class<Any>
) :
  Exception("No suitable constructor found. Was looking for either event constructor for class ${clazz.name} or default constructor")

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
