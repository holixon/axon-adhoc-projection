package io.holixon.axon.projection.adhoc

import io.holixon.axon.projection.adhoc.dummy.BankAccountAggregate
import io.holixon.axon.projection.adhoc.dummy.BankAccountCreatedEvent
import io.holixon.axon.projection.adhoc.dummy.CurrentBalanceImmutableModel
import io.holixon.axon.projection.adhoc.dummy.MoneyDepositedEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.axonframework.common.caching.Cache
import org.axonframework.eventhandling.GenericDomainEventMessage
import org.axonframework.eventsourcing.eventstore.EventStore
import org.axonframework.eventsourcing.eventstore.IteratorBackedDomainEventStream
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class ModelRepositoryTest {

  val cache = mockk<Cache>(relaxed = true)
  val eventStore = mockk<EventStore>()

  val repository = ModelRepository(eventStore, CurrentBalanceImmutableModel::class.java, cache)

  @Test
  fun `create model from scratch`() {
    val bankAccountId = UUID.randomUUID()
    mockEventStore(
      bankAccountId, listOf(
        BankAccountCreatedEvent(bankAccountId, "Alice"),
        MoneyDepositedEvent(bankAccountId, 100),
        MoneyDepositedEvent(bankAccountId, 30),
        MoneyDepositedEvent(bankAccountId, 10),
      )
    )

    val model = repository.findById(bankAccountId.toString())

    assertThat(model).isPresent

    verify { eventStore.readEvents(eq(bankAccountId.toString())) }
    verify { cache.containsKey(eq(bankAccountId.toString())) }
    verify { cache.put(eq(bankAccountId.toString()), any()) }
  }

  @Test
  fun `create model from scratch ignore snapshot`() {
    val repository = ModelRepository(eventStore, CurrentBalanceImmutableModel::class.java, cache, true)
    val bankAccountId = UUID.randomUUID()
    mockEventStore(
      bankAccountId, listOf(
        BankAccountCreatedEvent(bankAccountId, "Alice"),
        MoneyDepositedEvent(bankAccountId, 100),
        BankAccountAggregate(bankAccountId, "Alice", 100),
        MoneyDepositedEvent(bankAccountId, 30),
        MoneyDepositedEvent(bankAccountId, 10),
      )
    )

    val model = repository.findById(bankAccountId.toString())

    assertThat(model).isPresent

    verify { eventStore.readEvents(eq(bankAccountId.toString()), eq(0L)) }
    verify { cache.containsKey(eq(bankAccountId.toString())) }
    verify { cache.put(eq(bankAccountId.toString()), any()) }
  }

  @Test
  fun `id not found`() {
    val bankAccountId = UUID.randomUUID()
    mockEventStore(bankAccountId, listOf())

    val model = repository.findById(bankAccountId.toString())

    assertThat(model).isEmpty

    verify { eventStore.readEvents(eq(bankAccountId.toString())) }
    verify { cache.containsKey(eq(bankAccountId.toString())) }
  }

  @Test
  fun `model found in cache uptodate`() {
    val bankAccountId = UUID.randomUUID()
    mockEventStore(
      bankAccountId, listOf(
        BankAccountCreatedEvent(bankAccountId, "Alice")
      )
    )
    every { cache.containsKey(eq(bankAccountId.toString())) } returns true
    every { cache.get<String, CacheEntry<CurrentBalanceImmutableModel>>(eq(bankAccountId.toString())) } returns
      CacheEntry(
        bankAccountId.toString(),
        0,
        CurrentBalanceImmutableModel(BankAccountCreatedEvent(bankAccountId, "Alice"), Instant.now(), 0)
      )

    val model = repository.findById(bankAccountId.toString())

    assertThat(model).isPresent

    verify { eventStore.lastSequenceNumberFor(eq(bankAccountId.toString())) }
    verify(exactly = 0) { eventStore.readEvents(eq(bankAccountId.toString()), any()) }
    verify { cache.containsKey(eq(bankAccountId.toString())) }
    verify { cache.get(eq(bankAccountId.toString())) }
  }

  @Test
  fun `model found in cache too old`() {
    val bankAccountId = UUID.randomUUID()
    mockEventStore(
      bankAccountId, listOf(
        BankAccountCreatedEvent(bankAccountId, "Alice"),
        MoneyDepositedEvent(bankAccountId, 100)
      )
    )
    every { cache.containsKey(eq(bankAccountId.toString())) } returns true
    every { cache.get<String, CacheEntry<CurrentBalanceImmutableModel>>(eq(bankAccountId.toString())) } returns
      CacheEntry(
        bankAccountId.toString(),
        0,
        CurrentBalanceImmutableModel(BankAccountCreatedEvent(bankAccountId, "Alice"), Instant.now(), 0)
      )

    val model = repository.findById(bankAccountId.toString())

    assertThat(model).isPresent
    assertThat(model.get().version).isEqualTo(1L)

    verify { eventStore.lastSequenceNumberFor(eq(bankAccountId.toString())) }
    verify(exactly = 0) { eventStore.readEvents(eq(bankAccountId.toString())) }
    verify { cache.containsKey(eq(bankAccountId.toString())) }
    verify { cache.get(eq(bankAccountId.toString())) }
  }

  private fun mockEventStore(aggregateId: UUID, events: List<Any>) {
    var seqNo = -1L
    val eventStream = IteratorBackedDomainEventStream(events.map {
      GenericDomainEventMessage(it.javaClass.typeName, aggregateId.toString(), ++seqNo, it)
    }
      .iterator())

    every { eventStore.lastSequenceNumberFor(eq(aggregateId.toString())) } returns Optional.of(seqNo)
    every { eventStore.readEvents(eq(aggregateId.toString())) } returns eventStream
    every { eventStore.readEvents(eq(aggregateId.toString()), eq(0L)) } returns eventStream
    every { eventStore.readEvents(eq(aggregateId.toString()), any()) } answers {
      val firstSecNo = it.invocation.args[1] as Long
      eventStream.filter { event -> event.sequenceNumber >= firstSecNo }
    }
  }
}
