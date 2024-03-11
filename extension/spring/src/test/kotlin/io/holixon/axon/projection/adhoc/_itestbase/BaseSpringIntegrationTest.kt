package io.holixon.axon.projection.adhoc._itestbase

import io.holixon.axon.projection.adhoc.TestConfiguration
import org.axonframework.eventhandling.GenericDomainEventMessage
import org.axonframework.eventsourcing.eventstore.EventStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [TestConfiguration::class])
@ActiveProfiles("itest")
abstract class BaseSpringIntegrationTest {

  @Autowired
  lateinit var eventStore: EventStore

  protected fun <T> publishMessage(aggregateId: String, evt: T) {
    val lastSeqNo = eventStore.lastSequenceNumberFor(aggregateId).orElse(-1)

    eventStore.publish(GenericDomainEventMessage(evt!!::class.java.typeName, aggregateId, lastSeqNo + 1, evt))

  }
}
