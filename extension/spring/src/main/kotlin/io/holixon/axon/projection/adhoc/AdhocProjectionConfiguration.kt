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
import org.springframework.context.annotation.Lazy
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
      c.eventProcessing {
        it.registerEventHandler {adhocEventMessageHandler}
        it.assignProcessingGroup(AdhocEventMessageHandler.PROCESSING_GROUP, AdhocEventMessageHandler.PROCESSING_GROUP) }
    }
  }

  /**
   * The adhocEventMessageHandler.
   */
  @Bean
  fun adhocEventMessageHandler(@Lazy updatingModelRepository: List<UpdatingModelRepository<*>>) =
    AdhocEventMessageHandler(updatingModelRepository)
}
