package io.holixon.axon.projection.adhoc

import io.holixon.axon.projection.adhoc.dummy.BankAccountCreatedEvent
import io.holixon.axon.projection.adhoc.dummy.BankAccountEvent
import io.holixon.axon.projection.adhoc.dummy.CurrentBalanceImmutableModel
import io.holixon.axon.projection.adhoc.dummy.MoneyDepositedEvent
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.axonframework.common.Registration
import org.axonframework.common.caching.Cache
import org.axonframework.eventhandling.DomainEventMessage
import org.axonframework.eventhandling.GenericDomainEventMessage
import org.axonframework.eventsourcing.eventstore.EventStore
import org.axonframework.eventsourcing.eventstore.IteratorBackedDomainEventStream
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class UpdatingModelRepositoryTest {

  private val cache = mockk<Cache>().also {
    every { it.registerCacheEntryListener(any()) } returns Registration { true }
    every { it.containsKey(any()) } returns false
    every { it.get<String, CacheEntry<CurrentBalanceImmutableModel>>(any()) } returns null
    justRun { it.put(any(), any()) }
  }

  private val eventStore = mockk<EventStore>()

  private val repository =
    UpdatingModelRepository(eventStore, CurrentBalanceImmutableModel::class.java, ModelRepositoryConfig(cache = cache))

  @Test
  fun `update model in cache`() {
    val bankAccountId = UUID.randomUUID()
    mockEventStore(
      listOf(
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

    repository.on(convertToDomainEventMessage(MoneyDepositedEvent(bankAccountId, 100), 1L))

    val cacheEntrySlot = slot<CacheEntry<CurrentBalanceImmutableModel>>()

    verify(exactly = 0) { eventStore.readEvents(eq(bankAccountId.toString())) }
    verify { cache.put(eq(bankAccountId.toString()), capture(cacheEntrySlot)) }

    assertThat(cacheEntrySlot.captured.seqNo).isEqualTo(1)
    assertThat(cacheEntrySlot.captured.model.currentBalanceInEuroCent).isEqualTo(100)
  }


  @Test
  fun `update model in cache - apply missed events`() {
    val bankAccountId = UUID.randomUUID()
    mockEventStore(
      listOf(
        BankAccountCreatedEvent(bankAccountId, "Alice"),
        MoneyDepositedEvent(bankAccountId, 100),
        MoneyDepositedEvent(bankAccountId, 100),
        MoneyDepositedEvent(bankAccountId, 100),
      )
    )
    every { cache.containsKey(eq(bankAccountId.toString())) } returns true
    every { cache.get<String, CacheEntry<CurrentBalanceImmutableModel>>(eq(bankAccountId.toString())) } returns
      CacheEntry(
        bankAccountId.toString(),
        0,
        CurrentBalanceImmutableModel(BankAccountCreatedEvent(bankAccountId, "Alice"), Instant.now(), 0)
      )

    repository.on(convertToDomainEventMessage(MoneyDepositedEvent(bankAccountId, 100), 3L))

    val cacheEntrySlot = slot<CacheEntry<CurrentBalanceImmutableModel>>()

    verify { eventStore.readEvents(eq(bankAccountId.toString()), eq(1)) }
    verify { cache.put(eq(bankAccountId.toString()), capture(cacheEntrySlot)) }

    assertThat(cacheEntrySlot.captured.seqNo).isEqualTo(3)
    assertThat(cacheEntrySlot.captured.model.currentBalanceInEuroCent).isEqualTo(300)
  }

  @Test
  fun `force insert model in cache`() {
    val bankAccountId = UUID.randomUUID()
    mockEventStore(
      listOf(
        BankAccountCreatedEvent(bankAccountId, "Alice"),
        MoneyDepositedEvent(bankAccountId, 100),
        MoneyDepositedEvent(bankAccountId, 100),
        MoneyDepositedEvent(bankAccountId, 100),
      )
    )
    every { cache.containsKey(eq(bankAccountId.toString())) } returns false

    val forceInsertRepository = UpdatingModelRepository(
      eventStore,
      CurrentBalanceImmutableModel::class.java,
      ModelRepositoryConfig(cache = cache, forceCacheInsert = true)
    )

    forceInsertRepository.on(convertToDomainEventMessage(MoneyDepositedEvent(bankAccountId, 100), 3L))

    val cacheEntrySlot = slot<CacheEntry<CurrentBalanceImmutableModel>>()

    verify { eventStore.readEvents(eq(bankAccountId.toString())) }
    verify { cache.put(eq(bankAccountId.toString()), capture(cacheEntrySlot)) }

    assertThat(cacheEntrySlot.captured.seqNo).isEqualTo(3)
    assertThat(cacheEntrySlot.captured.model.currentBalanceInEuroCent).isEqualTo(300)
  }

  @Test
  fun `skip update model in cache - event is too new`() {
    val bankAccountId = UUID.randomUUID()
    every { cache.containsKey(eq(bankAccountId.toString())) } returns true
    every { cache.get<String, CacheEntry<CurrentBalanceImmutableModel>>(eq(bankAccountId.toString())) } returns
      CacheEntry(
        bankAccountId.toString(),
        1,
        CurrentBalanceImmutableModel(BankAccountCreatedEvent(bankAccountId, "Alice"), Instant.now(), 0)
          .on(MoneyDepositedEvent(bankAccountId, 100), Instant.now(), 0)
      )

    repository.on(convertToDomainEventMessage(MoneyDepositedEvent(bankAccountId, 100), 1L))

    val cacheEntrySlot = slot<CacheEntry<CurrentBalanceImmutableModel>>()

    // nothing should happen at all
    verify(exactly = 0) { eventStore.readEvents(eq(bankAccountId.toString())) }
    verify(exactly = 0) { cache.put(eq(bankAccountId.toString()), capture(cacheEntrySlot)) }
  }

  @Test
  fun `update model in cache calls listeners`() {
    val bankAccountId = UUID.randomUUID()
    mockEventStore(
      listOf(
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
    val listener = mockk<UpdatingModelRepository.ModelUpdatedListener<CurrentBalanceImmutableModel>>(relaxed = true)

    repository.addModelUpdatedListener(listener)
    repository.on(convertToDomainEventMessage(MoneyDepositedEvent(bankAccountId, 100), 1L))

    val modelSlot = slot<CurrentBalanceImmutableModel>()

    verify(exactly = 0) { eventStore.readEvents(eq(bankAccountId.toString())) }
    verify { listener.modelUpdated(capture(modelSlot)) }

    assertThat(modelSlot.captured.version).isEqualTo(1)
    assertThat(modelSlot.captured.currentBalanceInEuroCent).isEqualTo(100)
  }


  private fun mockEventStore(events: List<BankAccountEvent>) {
    var seqNo = -1L
    val eventStream = IteratorBackedDomainEventStream(events.map {
      convertToDomainEventMessage(it, ++seqNo)
    }
      .iterator())

    val aggregateId = events.first().bankAccountId
    every { eventStore.lastSequenceNumberFor(eq(aggregateId.toString())) } returns Optional.of(seqNo)
    every { eventStore.readEvents(eq(aggregateId.toString())) } returns eventStream
    every { eventStore.readEvents(eq(aggregateId.toString()), any()) } answers {
      val firstSecNo = it.invocation.args[1] as Long
      eventStream.filter { event -> event.sequenceNumber >= firstSecNo }
    }
  }

  private fun convertToDomainEventMessage(event: BankAccountEvent, seqNo: Long): DomainEventMessage<Any> =
    GenericDomainEventMessage(event.javaClass.typeName, event.bankAccountId.toString(), seqNo, event)
}
