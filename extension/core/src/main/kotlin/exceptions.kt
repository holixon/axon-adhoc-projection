package io.holixon.axon.selectivereplay

import java.lang.Exception

class NoSuitableConstructorFoundException(
  private val clazz: Class<Any>
) : Exception("No suitable constructor found. Was looking for either event constructor for class ${clazz.name} or default constructor")
