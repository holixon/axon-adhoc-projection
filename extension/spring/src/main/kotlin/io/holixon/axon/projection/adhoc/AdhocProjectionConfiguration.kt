package io.holixon.axon.projection.adhoc

import io.holixon.axon.projection.adhoc.AdhocEventMessageHandler
import io.holixon.axon.projection.adhoc.UpdatingModelRepository
import mu.KLogging
import org.axonframework.config.ConfigurerModule
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

/**
 * Autoconfiguration class for the event processors of the UpdatingModelRepository.
 *
 * Since we have our very custom EventMessageHandler, we need to register and configure it manually.
 */
@AutoConfiguration
class AdhocProjectionConfiguration {

  /**
   * Configurer module for the adhocEventMessageHandler.
   */
  @Bean
  fun adhocEventProcessingConfigurerModule(adhocEventMessageHandler: AdhocEventMessageHandler): ConfigurerModule {
    return ConfigurerModule { c ->
      c.registerEventHandler { adhocEventMessageHandler }
    }
  }

  /**
   * The adhocEventMessageHandler.
   */
  @Bean
  fun adhocEventMessageHandler() = AdhocEventMessageHandler()

  /**
   * Register the post initializer.
   */
  @Bean
  fun adhocEventMessageHandlerPostInitializer(
    updatingModelRepository: List<UpdatingModelRepository<*>>,
    adhocEventMessageHandler: AdhocEventMessageHandler
  ) = AdhocEventMessageHandlerPostInitializer(updatingModelRepository, adhocEventMessageHandler)
}

/**
 * We have a circular dependency here. The UpdatingModelRepositories require an eventStore,
 * but to start the Axon stack with the adhocEventMessageHandler we need the UpdatingModelRepositories.
 * <br /><br />
 * To solve this, we add the repositories to the eventMessageHandler just after the Spring context is finished.
 */
class AdhocEventMessageHandlerPostInitializer(
  private val updatingModelRepository: List<UpdatingModelRepository<*>>,
  private val adhocEventMessageHandler: AdhocEventMessageHandler
) : InitializingBean {

  companion object : KLogging()

  override fun afterPropertiesSet() {
    logger.debug { "Initialize AdhocEventMessageHandler" }
    updatingModelRepository.forEach {
      adhocEventMessageHandler.addRepository(it)
    }
  }
}
