package io.holixon.axon.projection.adhoc

import io.holixon.axon.projection.adhoc.dummy.CurrentBalanceImmutableModel
import org.axonframework.common.caching.Cache
import org.axonframework.common.transaction.TransactionManager
import org.axonframework.config.ConfigurationScopeAwareProvider
import org.axonframework.deadline.DeadlineManager
import org.axonframework.deadline.SimpleDeadlineManager
import org.axonframework.eventhandling.tokenstore.inmemory.InMemoryTokenStore
import org.axonframework.eventsourcing.eventstore.EventStore
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine
import org.axonframework.modelling.saga.repository.inmemory.InMemorySagaStore
import org.axonframework.spring.config.SpringConfigurer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary

@Configuration
@EnableAutoConfiguration
@Import(AdhocProjectionConfiguration::class)
class TestConfiguration {
  /**
   * Use in-memory event store in test.
   */
  @Bean
  fun evenStoreEngine() = InMemoryEventStorageEngine()

  /**
   * Use in-memory token store in test.
   */
  @Bean
  @Primary
  fun tokenStore() = InMemoryTokenStore()

  /**
   * Don't persist sagas in test.
   */
  @Bean
  fun sagaStore() = InMemorySagaStore()

  @Bean
  fun deadlineManager(configurer: SpringConfigurer, transactionManager: TransactionManager): DeadlineManager =
    SimpleDeadlineManager.builder().scopeAwareProvider(ConfigurationScopeAwareProvider(configurer.buildConfiguration()))
      .transactionManager(transactionManager).build()

  @Bean
  fun updateCache() = LRUCache(256)

  @Bean
  fun forceUpdateCache() = LRUCache(256)

  @Bean
  fun updatingCurrentBalanceModelRepository(eventStore: EventStore, @Qualifier("updateCache") cache: Cache) =
    UpdatingModelRepository(eventStore, CurrentBalanceImmutableModel::class.java, ModelRepositoryConfig(cache = cache))

  @Bean
  fun forceUpdatingCurrentBalanceModelRepository(eventStore: EventStore, @Qualifier("forceUpdateCache") cache: Cache) =
    UpdatingModelRepository(eventStore, CurrentBalanceImmutableModel::class.java, ModelRepositoryConfig(cache = cache, forceCacheInsert = true))
}
