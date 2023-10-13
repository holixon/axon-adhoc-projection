package io.holixon.axon.projection.adhoc


import io.holixon.axon.projection.adhoc.dummy.BankAccountCreatedEvent
import io.holixon.axon.projection.adhoc.dummy.CurrentBalanceImmutableModel
import io.holixon.axon.projection.adhoc.dummy.CurrentBalanceMutableModel
import io.holixon.axon.projection.adhoc.dummy.MoneyDepositedEvent
import io.holixon.axon.projection.adhoc.ModelFactory
import io.holixon.axon.projection.adhoc.ModelInspector
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.axonframework.eventhandling.GenericDomainEventMessage
import org.axonframework.eventsourcing.eventstore.DomainEventStream
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class ModelFactoryTest {

  val bankAccountId = UUID.randomUUID()

  @Test
  fun `creates model with first event`() {
    val factory = ModelFactory(
      ModelInspector(CurrentBalanceImmutableModel::class.java)
    )
    val eventStream = mockk<DomainEventStream>(relaxed = true)
    every { eventStream.peek() } returns
            GenericDomainEventMessage(
              BankAccountCreatedEvent::class.java.typeName,
              bankAccountId.toString(),
              0,
              BankAccountCreatedEvent(bankAccountId, "Alice")
            )

    factory.createInstanceFromStream(eventStream)

    verify { eventStream.next() }
  }

  @Test
  fun `creates model fails - no suitable constructor`() {
    val factory = ModelFactory(
      ModelInspector(CurrentBalanceImmutableModel::class.java)
    )
    val eventStream = mockk<DomainEventStream>(relaxed = true)
    every { eventStream.peek() } returns
            GenericDomainEventMessage(
              MoneyDepositedEvent::class.java.typeName,
              bankAccountId.toString(),
              0,
              MoneyDepositedEvent(bankAccountId, 123)
            )

    assertThrows<NoSuchMethodException> {
      factory.createInstanceFromStream(eventStream)
    }
  }

  @Test
  fun `creates model with default constructor event`() {
    val factory = ModelFactory(
      ModelInspector(CurrentBalanceMutableModel::class.java)
    )
    val eventStream = mockk<DomainEventStream>(relaxed = true)
    every { eventStream.peek() } returns
            GenericDomainEventMessage(
              BankAccountCreatedEvent::class.java.typeName,
              bankAccountId.toString(),
              0,
              BankAccountCreatedEvent(bankAccountId, "Alice")
            )

    factory.createInstanceFromStream(eventStream)

    verify(exactly = 0) { eventStream.next() }
  }

}
