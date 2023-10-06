package io.holixon.axon.selectivereplay

import java.lang.Exception
import java.lang.reflect.Method

class NoSuitableConstructorFoundException(
  private val clazz: Class<Any>
) : Exception("No suitable constructor found. Was looking for either event constructor for class ${clazz.name} or default constructor")

class DuplicateHandlerException(
  private val clazz: Class<*>,
  private val payloadClazz: Class<*>
) : Exception("At least two eventHandlers in the annotated class $clazz are capable of handling a payload of type $payloadClazz. This can't be handled")

class IllegalReturnTypeException(
  private val clazz: Class<*>,
  private val method: Method,
) : Exception("Method $method of class $clazz has disallowed return type ${method.returnType}. Only void or $clazz is allowed here")
