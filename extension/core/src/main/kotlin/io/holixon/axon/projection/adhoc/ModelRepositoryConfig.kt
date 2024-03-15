package io.holixon.axon.projection.adhoc

import org.axonframework.common.caching.Cache

/**
 * The model repository config.
 *
 * @param cache the cache implementation to use
 * @param cacheRefreshTime Time in ms a cache entry is considered up-to-date
 * @param forceCacheInsert configures the behavior when an event of an uncached entity is received
 */
data class ModelRepositoryConfig(
  /**
   * The cache implementation to use. Default is an LRU-Cache of size 1024
   */
  val cache: Cache = LRUCache(1024),
  /**
   * Time in ms a cache entry is considered up-to-date and no eventStore will be queried for new/missed events. Default is 0.<br/>
   * When using the <code>UpdatingModelRepository</code> consider a value other than 0 to use the advantage of the self-updating repo.
   */
  val cacheRefreshTime: Long = 0L,
  /**
   * By default, a DomainEventStream for an aggregate starts at the last snapshot event and ignores all prior events.
   * With this flag set to <code>true</code>, the stream will always start at sequenceNo 0.
   */
  val ignoreSnapshotEvents: Boolean = false,
  /**
   * Just for UpdatingModelRepository - Configures the behavior when an event of an uncached entity is received. When <code>false</code>
   * the event is ignored, when <code>true</code>, a full replay of this entity is performed and the result is added to the cache. Default is <code>false</code>
   */
  val forceCacheInsert: Boolean = false,
  ) {
  companion object {
    /**
     * Returns a config with default settings.
     */
    fun defaults() = ModelRepositoryConfig()
  }

  /**
   * The cache implementation to use. Default is an LRU-Cache of size 1024
   *
   * @param cache the cache to use
   */
  fun withCache(cache: Cache): ModelRepositoryConfig = copy(cache = cache)

  /**
   * Time in ms a cache entry is considered up-to-date and no eventStore will be queried for new/missed events. Default is 0.<br/>
   * When using the <code>UpdatingModelRepository</code> consider a value other than 0 to use the advantage of the self-updating repo.
   *
   * @param cacheRefreshTime the cacheRefreshTime
   */
  fun withCacheRefreshTime(cacheRefreshTime: Long): ModelRepositoryConfig = copy(cacheRefreshTime = cacheRefreshTime)

  /**
   * By default, a DomainEventStream for an aggregate starts at the last snapshot event and ignores all prior events.
   * With this flag set to <code>true</code>, the stream will always start at sequenceNo 0.
   *
   * @param ignoreSnapshotEvents if to ignore snapshots or not
   */
  fun withIgnoreSnapshotEvents(ignoreSnapshotEvents: Boolean): ModelRepositoryConfig = copy(ignoreSnapshotEvents = ignoreSnapshotEvents)

  /**
   * Just for UpdatingModelRepository - Configures the behavior when an event of an uncached entity is received. When <code>false</code>
   * the event is ignored, when <code>true</code>, a full replay of this entity is performed and the result is added to the cache. Default is <code>false</code>
   *
   * @param forceCacheInsert if new events always trigger cache inserts
   */
  fun withForceCacheInsert(forceCacheInsert: Boolean): ModelRepositoryConfig = copy(forceCacheInsert = forceCacheInsert)
}
