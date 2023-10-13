package io.holixon.axon.projection.adhoc

import org.axonframework.common.transaction.TransactionManager
import org.axonframework.config.ConfigurationScopeAwareProvider
import org.axonframework.deadline.DeadlineManager
import org.axonframework.deadline.SimpleDeadlineManager
import org.axonframework.eventhandling.tokenstore.inmemory.InMemoryTokenStore
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine
import org.axonframework.modelling.saga.repository.inmemory.InMemorySagaStore
import org.axonframework.spring.config.SpringConfigurer
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@EnableAutoConfiguration
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

}
