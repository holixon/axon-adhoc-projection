package io.holixon.axon.projection.adhoc

import io.holixon.axon.projection.adhoc.AdhocEventMessageHandler
import io.holixon.axon.projection.adhoc.UpdatingModelRepository
import mu.KLogging
import org.axonframework.config.ConfigurerModule
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component


@AutoConfiguration
class AdhocProjectionConfiguration {

  @Bean
  fun adhocEventProcessingConfigurerModule(adhocEventMessageHandler: AdhocEventMessageHandler): ConfigurerModule {
    return ConfigurerModule { c ->
      c.registerEventHandler { adhocEventMessageHandler }
    }
  }

  @Bean
  fun adhocEventMessageHandler() = AdhocEventMessageHandler()

  @Bean
  fun adhocEventMessageHandlerPostInitializer(
    updatingModelRepository: List<UpdatingModelRepository<*>>,
    adhocEventMessageHandler: AdhocEventMessageHandler
  ) = AdhocEventMessageHandlerPostInitializer(updatingModelRepository, adhocEventMessageHandler)
}
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
