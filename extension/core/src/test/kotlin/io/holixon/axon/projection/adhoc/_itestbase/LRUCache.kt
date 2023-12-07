package io.holixon.axon.projection.adhoc._itestbase

import org.axonframework.common.Registration
import org.axonframework.common.caching.Cache

class LRUCache(val maxSize: Int) : Cache {

  private val internalCache: MutableMap<Any?, Any?> = object : LinkedHashMap<Any?, Any?>(0, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Any?, Any?>?): Boolean {
      return size > maxSize
    }
  }
  override fun <K : Any?, V : Any?> get(key: K): V {
    return internalCache[key] as V
  }

  override fun put(key: Any?, value: Any?) {
    internalCache[key] = value
  }

  override fun putIfAbsent(key: Any?, value: Any?): Boolean =
    internalCache.putIfAbsent(key, value) == null

  override fun remove(key: Any?): Boolean = internalCache.remove(key) != null

  override fun containsKey(key: Any?): Boolean = internalCache.containsKey(key)

  override fun registerCacheEntryListener(cacheEntryListener: Cache.EntryListener?): Registration = Registration { true }
}
