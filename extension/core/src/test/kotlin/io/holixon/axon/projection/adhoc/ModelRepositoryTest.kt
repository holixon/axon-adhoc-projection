package io.holixon.axon.projection.adhoc

import io.holixon.axon.projection.adhoc.dummy.BankAccountCreatedEvent
import io.holixon.axon.projection.adhoc.dummy.CurrentBalanceImmutableModel
import io.holixon.axon.projection.adhoc.dummy.MoneyDepositedEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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

  val repository = ModelRepository(eventStore, CurrentBalanceImmutableModel::class.java, ModelRepositoryConfig(cache = cache))

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
  fun `create model versioned from scratch`() {
    val bankAccountId = UUID.randomUUID()
    mockEventStore(
      bankAccountId, listOf(
        BankAccountCreatedEvent(bankAccountId, "Alice"),
        MoneyDepositedEvent(bankAccountId, 100),
        MoneyDepositedEvent(bankAccountId, 30),
        MoneyDepositedEvent(bankAccountId, 10),
      )
    )

    val model = repository.readModelFromScratch(bankAccountId.toString(), 1)

    assertThat(model).isPresent

    val cacheEntrySlot = slot<CacheEntry<CurrentBalanceImmutableModel>>()

    verify { eventStore.readEvents(eq(bankAccountId.toString())) }
    verify { cache.put(eq(bankAccountId.toString()), capture(cacheEntrySlot)) }

    assertThat(cacheEntrySlot.captured.seqNo).isEqualTo(1)
    assertThat(cacheEntrySlot.captured.model.currentBalanceInEuroCent).isEqualTo(100)
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
  fun `model found in cache up to date`() {
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
  fun `model found in cache not up to date`() {
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

  @Test
  fun `model found in cache and cache entry is new enough`() {
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
        CurrentBalanceImmutableModel(BankAccountCreatedEvent(bankAccountId, "Alice"), Instant.now(), 0),
        Instant.now()
      )

    val repositoryWithCacheRefreshTimes =
      ModelRepository(eventStore, CurrentBalanceImmutableModel::class.java, ModelRepositoryConfig(cache = cache, cacheRefreshTime = 10000L))

    val model = repositoryWithCacheRefreshTimes.findById(bankAccountId.toString())

    assertThat(model).isPresent
    assertThat(model.get().version).isEqualTo(0L)

    verify(exactly = 0) { eventStore.lastSequenceNumberFor(eq(bankAccountId.toString())) }
    verify(exactly = 0) { eventStore.readEvents(eq(bankAccountId.toString())) }
    verify { cache.containsKey(eq(bankAccountId.toString())) }
    verify { cache.get(eq(bankAccountId.toString())) }
    verify(exactly = 0) { cache.put(eq(bankAccountId.toString()), any()) }
  }

  @Test
  fun `model found in cache up to date but cache entry is too old`() {
    val bankAccountId = UUID.randomUUID()
    mockEventStore(
      bankAccountId, listOf(
        BankAccountCreatedEvent(bankAccountId, "Alice"),
      )
    )
    every { cache.containsKey(eq(bankAccountId.toString())) } returns true
    every { cache.get<String, CacheEntry<CurrentBalanceImmutableModel>>(eq(bankAccountId.toString())) } returns
      CacheEntry(
        bankAccountId.toString(),
        0,
        CurrentBalanceImmutableModel(BankAccountCreatedEvent(bankAccountId, "Alice"), Instant.now(), 0),
        Instant.now().minusMillis(20000)
      )

    val repositoryWithCacheRefreshTimes =
      ModelRepository(eventStore, CurrentBalanceImmutableModel::class.java, ModelRepositoryConfig(cache = cache, cacheRefreshTime = 10000L))

    val model = repositoryWithCacheRefreshTimes.findById(bankAccountId.toString())

    assertThat(model).isPresent
    assertThat(model.get().version).isEqualTo(0L)

    verify { eventStore.lastSequenceNumberFor(eq(bankAccountId.toString())) }
    verify(exactly = 0) { eventStore.readEvents(eq(bankAccountId.toString())) }
    verify { cache.containsKey(eq(bankAccountId.toString())) }
    verify { cache.get(eq(bankAccountId.toString())) }
    verify { cache.put(eq(bankAccountId.toString()), any()) }
  }

  private fun mockEventStore(aggregateId: UUID, events: List<Any>) {
    var seqNo = -1L
    val eventStream = IteratorBackedDomainEventStream(events.map {
      GenericDomainEventMessage(it.javaClass.typeName, aggregateId.toString(), ++seqNo, it)
    }
      .iterator())

    every { eventStore.lastSequenceNumberFor(eq(aggregateId.toString())) } returns Optional.of(seqNo)
    every { eventStore.readEvents(eq(aggregateId.toString())) } returns eventStream
    every { eventStore.readEvents(eq(aggregateId.toString()), any()) } answers {
      val firstSecNo = it.invocation.args[1] as Long
      eventStream.filter { event -> event.sequenceNumber >= firstSecNo }
    }
  }
}
