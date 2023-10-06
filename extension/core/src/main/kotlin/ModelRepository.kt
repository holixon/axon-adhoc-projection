package io.holixon.axon.selectivereplay

import mu.KLogging
import org.axonframework.common.caching.Cache
import org.axonframework.common.caching.Cache.EntryListenerAdapter
import org.axonframework.common.caching.NoCache
import org.axonframework.eventsourcing.eventstore.EventStore
import java.util.*

open class ModelRepository<T : Any>(
  private val eventStore : EventStore,
  private val clazz: Class<T>,
  private val cache: Cache = NoCache.INSTANCE
) {
  companion object : KLogging()

  init {
    cache.registerCacheEntryListener(CacheEntryListener(clazz.simpleName))
  }

  private val modelInspector = ModelInspector(clazz)
  private val modelFactory = ModelFactory(modelInspector)
  private val eventApplier = EventApplier(modelInspector)

  fun findById(aggregateId: String) : Optional<T> {
    return if (cache.containsKey(aggregateId)) {
      Optional.of(readAndUpdateModelFromCache(aggregateId))
    } else {
      readModelFromScratch(aggregateId)
    }
  }

  internal fun readModelFromScratch(aggregateId: String) : Optional<T> {
    val cacheEntry = createCacheEntryFromScratch(aggregateId)

    return if (cacheEntry != null) {
      cache.put(aggregateId, cacheEntry)
      Optional.ofNullable(cacheEntry.model)
    } else {
      Optional.empty()
    }
  }

  internal fun createCacheEntryFromScratch(aggregateId: String) : CacheEntry<T>? {
    logger.debug { "Reading model for ${clazz.simpleName} with ID $aggregateId from scratch" }
    val events = eventStore.readEvents(aggregateId)
    if (!events.hasNext()) {
      return null
    }

    var model: T = modelFactory.createInstanceFromStream(events)

    var lastSeqNo = 0L
    events.forEachRemaining { event ->
      logger.debug { "Reading event ${event.payloadType.simpleName} with seqNo ${event.sequenceNumber} for aggregate ID $aggregateId" }
      lastSeqNo = event.sequenceNumber
      model = eventApplier.applyEvent(model, event)
    }

    return CacheEntry(aggregateId, lastSeqNo, model)
  }


  internal fun readAndUpdateModelFromCache(aggregateId: String) : T {
    val currentCacheEntry = cache.get<String, CacheEntry<T>>(aggregateId)
    logger.debug { "Reading cached model for ${clazz.simpleName} with ID $aggregateId and seqNo ${currentCacheEntry.seqNo}" }

    val lastSeqNo = eventStore.lastSequenceNumberFor(aggregateId).orElseThrow()

    if (lastSeqNo == currentCacheEntry.seqNo) {
      // cache still uptodate, can directly return cache entry
      return currentCacheEntry.model
    }
    var model: T = currentCacheEntry.model

    val events = eventStore.readEvents(aggregateId, currentCacheEntry.seqNo + 1)

    events.forEachRemaining { event ->
      logger.debug { "Reading event ${event.payloadType.simpleName} with seqNo ${event.sequenceNumber} for aggregate ID $aggregateId" }
      model = eventApplier.applyEvent(model, event)
    }

    val newCacheEntry = CacheEntry(aggregateId, lastSeqNo, model)
    cache.put(aggregateId, newCacheEntry)

    return newCacheEntry.model
  }

  internal class CacheEntryListener(
    private val cacheName: String
  ) : EntryListenerAdapter() {
    companion object: KLogging()

    override fun onEntryCreated(key: Any?, value: Any?) {
      logger.debug { "$cacheName: Cache entry $key created" }
    }

    override fun onEntryExpired(key: Any?) {
      logger.debug { "$cacheName: Cache entry $key expired" }
    }

    override fun onEntryRemoved(key: Any?) {
      logger.debug { "$cacheName: Cache entry $key removed" }
    }
  }

}

internal class CacheEntry<T>(
  val aggregateId: String,
  val seqNo: Long,
  val model: T
)

