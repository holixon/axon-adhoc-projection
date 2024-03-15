package io.holixon.axon.projection.adhoc.example

import io.holixon.axon.projection.adhoc.AdhocEventMessageHandler
import org.axonframework.config.Configurer
import org.axonframework.config.ConfigurerModule
import org.axonframework.config.EventProcessingConfigurer
import org.axonframework.eventhandling.TrackingEventProcessorConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean


@SpringBootApplication
class KotlinBankingApplication {

  @Bean
  fun segmentCountConfigurerModule(): ConfigurerModule? {
    return ConfigurerModule { configurer: Configurer ->
      configurer.eventProcessing { processingConfigurer: EventProcessingConfigurer ->
        processingConfigurer.registerTrackingEventProcessorConfiguration(AdhocEventMessageHandler.PROCESSING_GROUP) {
          TrackingEventProcessorConfiguration.forParallelProcessing(4).andInitialTrackingToken { it.createHeadToken() }
        }
      }
    }
  }
}

fun main(args: Array<String>) {
  runApplication<KotlinBankingApplication>(*args)
}
