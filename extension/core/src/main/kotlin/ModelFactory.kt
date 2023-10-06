package io.holixon.axon.selectivereplay

class ModelFactory<T>(
  private val modelInspector: ModelInspector<T>
) {
  fun createInstance(firstEvent: Any): T {
    modelInspector.findConstructor(firstEvent.javaClass)?.let {
      return it.newInstance(firstEvent)
    }

    try {
      return modelInspector.getDefaultConstructor().newInstance(firstEvent)
    } catch (e: NoSuchMethodException) {
      throw NoSuitableConstructorFoundException(firstEvent.javaClass)
    }

  }
}
