package io.holixon.axon.selectivereplay

import java.lang.reflect.Constructor

class ModelInspector<T>(
  val clazz: Class<T>
) {
  fun getDefaultConstructor(): Constructor<T> {
    return clazz.getConstructor()
  }

  fun <E> findConstructor(eventClass: Class<E>): Constructor<T>? {
    return try {
      clazz.getConstructor(eventClass)
    } catch (e: NoSuchMethodException) {
      return null
    }
  }

  fun <E> findEventHandler(eventClazz: Class<E>): EventHandler<T>? {
    clazz.methods.find { it.parameterTypes.size == 1 && it.parameterTypes[0] == eventClazz}
      ?.let { return EventHandler(it, clazz) }

    return null
  }


}
