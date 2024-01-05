package io.holixon.axon.projection.adhoc

import io.holixon.axon.projection.adhoc.dummy.*
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.axonframework.eventhandling.DomainEventMessage
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.SequenceNumber
import org.axonframework.messaging.annotation.MessageHandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EventApplierTest {

  @Test
  fun `event is relevant for model`() {
    val eventApplier = EventApplier(ModelInspector(CurrentBalanceImmutableModel::class.java))

    val event = mockk<DomainEventMessage<MoneyDepositedEvent>>()
    every { event.payloadType } returns MoneyDepositedEvent::class.java

    assertThat(eventApplier.isRelevant(event)).isTrue()
  }

  @Test
  fun `event is not relevant for model`() {
    val eventApplier = EventApplier(ModelInspector(CurrentBalanceImmutableModel::class.java))

    val event = mockk<DomainEventMessage<OwnerChangedEvent>>()
    every { event.payloadType } returns OwnerChangedEvent::class.java

    assertThat(eventApplier.isRelevant(event)).isFalse()
  }
}
