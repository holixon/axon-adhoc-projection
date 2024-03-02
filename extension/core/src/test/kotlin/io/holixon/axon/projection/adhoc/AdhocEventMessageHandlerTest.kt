package io.holixon.axon.projection.adhoc

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.axonframework.eventhandling.DomainEventMessage
import org.axonframework.eventhandling.GenericEventMessage
import org.junit.jupiter.api.Test

class AdhocEventMessageHandlerTest {

  @Test
  fun handle() {
    val message = mockk<DomainEventMessage<*>>()
    val repository1 = mockk<UpdatingModelRepository<*>>(relaxed = true)
    every { repository1.canHandleMessage(any()) } returns false
    val repository2 = mockk<UpdatingModelRepository<*>>(relaxed = true)
    every { repository2.canHandleMessage(any()) } returns true

    val handler = AdhocEventMessageHandler()
    handler.addRepository(repository1)
    handler.addRepository(repository2)

    assertThat(handler.handle(message))

    verify(exactly = 0) { repository1.on(message) }
    verify { repository2.on(message) }
  }

  @Test
  fun `cannot handle without repositories`() {
    val message = mockk<DomainEventMessage<*>>()

    val handler = AdhocEventMessageHandler()

    assertThat(handler.canHandle(message)).isFalse()
  }

  @Test
  fun `cannot handle non-domain message`() {
    val message = mockk<GenericEventMessage<*>>()
    val repository1 = mockk<UpdatingModelRepository<*>>()
    every { repository1.canHandleMessage(any()) } returns true

    val handler = AdhocEventMessageHandler()
    handler.addRepository(repository1)

    assertThat(handler.canHandle(message)).isFalse()
  }

  @Test
  fun `can handle with one positive repository`() {
    val message = mockk<DomainEventMessage<*>>()
    val repository1 = mockk<UpdatingModelRepository<*>>()
    every { repository1.canHandleMessage(any()) } returns true

    val handler = AdhocEventMessageHandler()
    handler.addRepository(repository1)

    assertThat(handler.canHandle(message)).isTrue()
  }

  @Test
  fun `cannot handle with one negative repository`() {
    val message = mockk<DomainEventMessage<*>>()
    val repository1 = mockk<UpdatingModelRepository<*>>()
    every { repository1.canHandleMessage(any()) } returns false

    val handler = AdhocEventMessageHandler()
    handler.addRepository(repository1)

    assertThat(handler.canHandle(message)).isFalse()
  }

  @Test
  fun `can handle with one positive and one negative repository`() {
    val message = mockk<DomainEventMessage<*>>()
    val repository1 = mockk<UpdatingModelRepository<*>>()
    every { repository1.canHandleMessage(any()) } returns false
    val repository2 = mockk<UpdatingModelRepository<*>>()
    every { repository2.canHandleMessage(any()) } returns true

    val handler = AdhocEventMessageHandler()
    handler.addRepository(repository1)
    handler.addRepository(repository2)

    assertThat(handler.canHandle(message)).isTrue()
  }

  @Test
  fun `do not handle with one positive and one negative repository`() {
    val message = mockk<GenericEventMessage<*>>()
    val repository1 = mockk<UpdatingModelRepository<*>>()
    every { repository1.canHandleMessage(any()) } returns false
    val repository2 = mockk<UpdatingModelRepository<*>>()
    every { repository2.canHandleMessage(any()) } returns true

    val handler = AdhocEventMessageHandler()
    handler.addRepository(repository1)
    handler.addRepository(repository2)

    assertThat(handler.handle(message))

    verify(exactly = 0) { repository1.on(any()) }
    verify(exactly = 0) { repository2.on(any()) }
  }

  @Test
  fun `throw error when one repository threw exception`() {
    val message = mockk<DomainEventMessage<*>>()
    val repository1 = mockk<UpdatingModelRepository<*>>(relaxed = true)
    every { repository1.canHandleMessage(any()) } returns true
    every { repository1.on(any()) } throws Exception("test")
    val repository2 = mockk<UpdatingModelRepository<*>>(relaxed = true)
    every { repository2.canHandleMessage(any()) } returns true

    val handler = AdhocEventMessageHandler()
    handler.addRepository(repository1)
    handler.addRepository(repository2)

    assertThatThrownBy {
      assertThat(handler.handle(message))
    }.isInstanceOf(AdhocEventMessageHandler.ProcessingFailureException::class.java)
      .hasMessage("Failed to apply event to following UpdatingModelRepositories: [UpdatingModelRepository]")

    verify { repository1.on(any()) }
    verify { repository2.on(any()) }
  }
}
