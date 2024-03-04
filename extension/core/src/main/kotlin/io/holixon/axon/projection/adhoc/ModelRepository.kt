package io.holixon.axon.projection.adhoc

import mu.KLogging
import org.axonframework.common.caching.Cache
import org.axonframework.common.caching.Cache.EntryListenerAdapter
import org.axonframework.common.caching.NoCache
import org.axonframework.eventsourcing.eventstore.EventStore
import java.time.Instant
import java.util.*

/**
 * Central component for building ad-hoc projections. THe ModelRepository looks for methods and constructors in the modelClass annotated
 * with Axon's @MessageHandler. When constructing the model instance the domain events will be applied using the annotated methods.
 * The result is put into the given cache.<br/>
 * <br/>
 * When a cached version is found, by default the Axon server will always be called for new events. With the config parameter
 * <code>cacheRefreshTime</code> a duration (in ms) can be defined for which the cached entry will be considered as up-to-date without
 * checking for new events in the event store.
 *
 * @param eventStore the axon eventStore to use
 * @param modelClass the model class type to build the projection on
 * @param config the repository config to apply
 */
open class ModelRepository<T : Any>(
  private val eventStore: EventStore,
  private val modelClass: Class<T>,
  protected val config: ModelRepositoryConfig = ModelRepositoryConfig.defaults(),
) {
  companion object : KLogging()

  protected val cache: Cache = config.cache
  private val modelInspector = ModelInspector(modelClass)
  private val modelFactory = ModelFactory(modelInspector)
  protected val eventApplier = EventApplier(modelInspector)

  init {
    require(config.cacheRefreshTime >= 0L) { "The cache refresh time must not be negative" }
  }

  /**
   * Looks for all events of this aggregateId and constructs a model instance. If a cached version exists, only the remaining new events
   * are applied to the cached instance. The final model instance will again be put into the cache.
   *
   * @param aggregateId the aggregateId
   * @return either the built model or an empty optional if the aggregateId had no events
   */
  fun findById(aggregateId: String): Optional<T> {
    return if (cache.containsKey(aggregateId)) {
      val cacheEntry: CacheEntry<T> = cache[aggregateId]
      if (config.cacheRefreshTime == 0L || cacheEntry.created.isBefore(Instant.now().minusMillis(config.cacheRefreshTime))) {
        Optional.of(readAndUpdateModelFromCache(aggregateId))
      } else {
        Optional.of(cacheEntry.model)
      }
    } else {
      readModelFromScratch(aggregateId)
    }
  }

  internal fun readModelFromScratch(aggregateId: String): Optional<T> {
    val cacheEntry = createCacheEntryFromScratch(aggregateId)

    return if (cacheEntry != null) {
      cache.put(aggregateId, cacheEntry)
      Optional.ofNullable(cacheEntry.model)
    } else {
      Optional.empty()
    }
  }

  private fun createCacheEntryFromScratch(aggregateId: String): CacheEntry<T>? {
    logger.debug { "Reading model for ${modelClass.simpleName} with ID $aggregateId from scratch" }
    val events = if (config.ignoreSnapshotEvents) {
      // read from the very first event and not starting with latest snapshot
      eventStore.readEvents(aggregateId, 0L)
    } else {
      eventStore.readEvents(aggregateId)
    }
    if (!events.hasNext()) {
      return null
    }

    var model: T = modelFactory.createInstanceFromStream(events)

    var lastSeqNo = 0L
    events
      .forEachRemaining { event ->
        logger.trace { "Reading event ${event.payloadType.simpleName} with seqNo ${event.sequenceNumber} for aggregate ID $aggregateId" }
        lastSeqNo = event.sequenceNumber
        model = eventApplier.applyEvent(model, event)
      }

    return CacheEntry(aggregateId, lastSeqNo, model)
  }


  internal fun readAndUpdateModelFromCache(aggregateId: String): T {
    val currentCacheEntry = cache.get<String, CacheEntry<T>>(aggregateId)
    logger.debug { "Reading cached model for ${modelClass.simpleName} with ID $aggregateId and seqNo ${currentCacheEntry.seqNo}" }

    val lastSeqNo = eventStore.lastSequenceNumberFor(aggregateId).orElseThrow()

    // cache still uptodate, can directly return cache entry
    if (lastSeqNo == currentCacheEntry.seqNo) {
      // refresh cache timestamp only when too old
      if (config.cacheRefreshTime != 0L && currentCacheEntry.created.isBefore(Instant.now().minusMillis(config.cacheRefreshTime))) {
        cache.put(aggregateId, currentCacheEntry.copy(created = Instant.now()))
      }

      return currentCacheEntry.model
    }
    var model: T = currentCacheEntry.model

    val events = eventStore.readEvents(aggregateId, currentCacheEntry.seqNo + 1)

    events
      .forEachRemaining { event ->
        logger.trace { "Reading event ${event.payloadType.simpleName} with seqNo ${event.sequenceNumber} for aggregate ID $aggregateId" }
        model = eventApplier.applyEvent(model, event)
      }

    val newCacheEntry = CacheEntry(aggregateId, lastSeqNo, model)
    cache.put(aggregateId, newCacheEntry)

    return newCacheEntry.model
  }

  /**
   * Clears the used cache of any cacheEntries.
   */
  fun resetCache() {
    this.cache.removeAll()
  }
}

/**
 * A cache entry for a model.
 */
data class CacheEntry<T>(
  val aggregateId: String,
  val seqNo: Long,
  val model: T,
  val created: Instant = Instant.now(),
)

