package io.holixon.axon.projection.adhoc

import mu.KLogging
import org.axonframework.eventhandling.DomainEventMessage
import org.axonframework.eventsourcing.eventstore.EventStore

/**
 * Enhancement class for ModelRepository. The UpdatingModelRepository adds a tracking event processor
 * which is able to update existing cache entries with a received event so all cached entries are always up-to-date.
 *
 * Usage: The UpdatingModelRepository must be registered as an EventHandler component to the application.
 * The processor usually should start at the head of the stream or have a persisted token.
 *
 * With the parameter <code>forceCacheInsert</code> the repository will force a full and up-to-date cache
 * entry creation for events where there is no cache entry.
 *
 * @param eventStore the axon eventStore to use
 * @param modelClass the model class type to build the projection on
 * @param config the repository config to apply
 */
open class UpdatingModelRepository<T : Any>(
  eventStore: EventStore,
  modelClass: Class<T>,
  config: ModelRepositoryConfig = ModelRepositoryConfig.defaults(),
) : ModelRepository<T>(eventStore, modelClass, config) {

  companion object : KLogging()

  private var modelUpdatedListeners: MutableList<ModelUpdatedListener<T>> = mutableListOf()

  /**
   * Adds a listener to the repository firing every time a cached instance was created or changed
   *
   * @param listener the listener to add
   */
  fun addModelUpdatedListener(listener: ModelUpdatedListener<T>) {
    modelUpdatedListeners.add(listener)
  }

  /**
   * Removes a previously added listener
   *
   * @param listener the listener to add
   */
  fun removeModelUpdatedListener(listener: ModelUpdatedListener<T>) {
    modelUpdatedListeners.remove(listener)
  }

  internal fun canHandleMessage(eventMessage: DomainEventMessage<*>): Boolean {
    return eventApplier.isRelevant(eventMessage)
  }

  internal fun on(eventMessage: DomainEventMessage<*>) {
    logger.debug { "Received event ${eventMessage.payloadType.simpleName} ${eventMessage.sequenceNumber} of aggregate ${eventMessage.aggregateIdentifier}" }

    val cacheEntry: CacheEntry<T>? = cache.get(eventMessage.aggregateIdentifier)

    if (cacheEntry != null) {
      handleCacheEntry(eventMessage, cacheEntry)
    } else if (config.forceCacheInsert) {
      // create cache entry up to this event and store it
      logger.debug { "Aggregate ${eventMessage.aggregateIdentifier} was not found in cache, replay up to seqNo ${eventMessage.sequenceNumber} and store in cache" }
      readModelFromScratch(eventMessage.aggregateIdentifier)
        .ifPresent { callEventListener(it) }
    }
  }

  private fun handleCacheEntry(eventMessage: DomainEventMessage<*>, cacheEntry: CacheEntry<T>) {
    val seqNo = eventMessage.sequenceNumber
    val aggregateIdentifier = eventMessage.aggregateIdentifier

    if (cacheEntry.seqNo + 1 == seqNo) {
      logger.trace { "Updating cache entry for aggregate $aggregateIdentifier seqNo $seqNo" }
      // cacheEntry is exactly one event behind, apply the event and update cache
      if (eventApplier.isRelevant(eventMessage)) {
        val newCacheEntry = applySingleEventToCacheEntry(cacheEntry, eventMessage)
        cache.put(aggregateIdentifier, newCacheEntry)
        callEventListener(newCacheEntry.model)
      }
    } else if (cacheEntry.seqNo + 1 < seqNo) {
      logger.debug { "Cached aggregate $aggregateIdentifier is of version ${cacheEntry.seqNo} which is too old for new event $seqNo - replaying missing events" }
      // did we miss some events? Update the cache entry with the whole stream.
      // If a replay is in progress, this can lead to many ignored events for this aggregate but is still faster than just replaying to the current seqNo multiple times
      readAndUpdateModelFromCache(aggregateIdentifier)
        .also { callEventListener(it) }
    } else if (cacheEntry.seqNo >= seqNo) {
      // must be a replay, ignore it
      logger.debug { "Cached aggregate $aggregateIdentifier is of version ${cacheEntry.seqNo} which is too new for new event $seqNo - seems to be a replay in progress, ignoring event" }
    }
  }

  private fun applySingleEventToCacheEntry(cacheEntry: CacheEntry<T>, eventMessage: DomainEventMessage<*>): CacheEntry<T> {
    val newModel = eventApplier.applyEvent(cacheEntry.model, eventMessage)

    return CacheEntry(eventMessage.aggregateIdentifier, eventMessage.sequenceNumber, newModel)
  }

  private fun callEventListener(model: T) {
    modelUpdatedListeners.forEach { it.modelUpdated(model) }
  }

  /**
   * A listener to the repository firing every time a cached instance was created or changed
   */
  @FunctionalInterface
  fun interface ModelUpdatedListener<T> {

    /**
     * Will be called with the new model instance every time the model instance is changed.
     *
     * @param model the new model instance
     */
    fun modelUpdated(model: T)
  }
}
