package io.holixon.axon.projection.adhoc

import io.holixon.axon.projection.adhoc._itestbase.BaseSpringIntegrationTest
import io.holixon.axon.projection.adhoc.dummy.BankAccountCreatedEvent
import io.holixon.axon.projection.adhoc.dummy.CurrentBalanceImmutableModel
import io.holixon.axon.projection.adhoc.dummy.MoneyDepositedEvent
import io.holixon.axon.projection.adhoc.dummy.MoneyWithdrawnEvent
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.axonframework.common.caching.Cache
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import java.util.*

class UpdatingModelRepositoryITest : BaseSpringIntegrationTest() {

  @Autowired
  @Qualifier("updatingCurrentBalanceModelRepository")
  lateinit var repository: UpdatingModelRepository<CurrentBalanceImmutableModel>

  @Autowired
  @Qualifier("forceUpdatingCurrentBalanceModelRepository")
  lateinit var forceUpdatingRepository: UpdatingModelRepository<CurrentBalanceImmutableModel>

  @Autowired
  @Qualifier("updateCache")
  lateinit var updateCache: Cache

  @Autowired
  @Qualifier("forceUpdateCache")
  lateinit var forceUpdateCache: Cache

  val cacheUpdateLogger = CacheUpdateLogger()
  val forcedCacheUpdateLogger = CacheUpdateLogger()

  @BeforeEach
  fun beforeEach() {
    repository.addModelUpdatedListener(cacheUpdateLogger)
    forceUpdatingRepository.addModelUpdatedListener(forcedCacheUpdateLogger)
  }

  @AfterEach
  fun afterEach() {
    repository.removeModelUpdatedListener(cacheUpdateLogger)
    forceUpdatingRepository.removeModelUpdatedListener(forcedCacheUpdateLogger)
  }

  @Test
  fun `immutable projection can be loaded`() {
    val bankAccountId = UUID.randomUUID()
    publishMessage(bankAccountId.toString(), BankAccountCreatedEvent(bankAccountId, ""))
    publishMessage(bankAccountId.toString(), MoneyDepositedEvent(bankAccountId, 100))
    publishMessage(bankAccountId.toString(), MoneyWithdrawnEvent(bankAccountId, 50))

    val model = repository.findById(bankAccountId.toString())

    assertThat(model).isPresent
    assertThat(model.get().currentBalanceInEuroCent).isEqualTo(50)
    assertThat(model.get().version).isEqualTo(2)
  }

  @Test
  fun `cache is updated automatically`() {
    val bankAccountId = UUID.randomUUID()
    publishMessage(bankAccountId.toString(), BankAccountCreatedEvent(bankAccountId, ""))
    publishMessage(bankAccountId.toString(), MoneyDepositedEvent(bankAccountId, 100))
    publishMessage(bankAccountId.toString(), MoneyWithdrawnEvent(bankAccountId, 50))

    assertThat(updateCache.containsKey(bankAccountId.toString())).isFalse()

    repository.findById(bankAccountId.toString())

    assertThat(updateCache.containsKey(bankAccountId.toString())).isTrue()

    publishMessage(bankAccountId.toString(), MoneyWithdrawnEvent(bankAccountId, 40))

    await untilAsserted {
      val cachedModel: CacheEntry<CurrentBalanceImmutableModel> = updateCache[bankAccountId.toString()]
      assertThat(cachedModel.seqNo).isEqualTo(3)
      assertThat(cachedModel.model.currentBalanceInEuroCent).isEqualTo(10)

      assertThat(cacheUpdateLogger.updates).containsKey(bankAccountId)
      assertThat(cacheUpdateLogger.updates[bankAccountId]).hasSize(1)
      assertThat(cacheUpdateLogger.updates[bankAccountId]!![0].currentBalanceInEuroCent).isEqualTo(10)
    }
  }

  @Test
  fun `cache is inserted automatically on forceInsert=true`() {
    val bankAccountId = UUID.randomUUID()
    publishMessage(bankAccountId.toString(), BankAccountCreatedEvent(bankAccountId, ""))
    publishMessage(bankAccountId.toString(), MoneyDepositedEvent(bankAccountId, 100))
    publishMessage(bankAccountId.toString(), MoneyWithdrawnEvent(bankAccountId, 50))

    await untilAsserted {
      assertThat(forceUpdateCache.containsKey(bankAccountId.toString())).isTrue()
      val cachedModel: CacheEntry<CurrentBalanceImmutableModel> = forceUpdateCache[bankAccountId.toString()]
      assertThat(cachedModel.seqNo).isEqualTo(2)
      assertThat(cachedModel.model.currentBalanceInEuroCent).isEqualTo(50)

      assertThat(forcedCacheUpdateLogger.updates).containsKey(bankAccountId)
    }
  }

  class CacheUpdateLogger : UpdatingModelRepository.ModelUpdatedListener<CurrentBalanceImmutableModel> {
    val updates = mutableMapOf<UUID, MutableList<CurrentBalanceImmutableModel>>()

    override fun modelUpdated(model: CurrentBalanceImmutableModel) {
      updates.putIfAbsent(model.bankAccountId, ArrayList())

      updates[model.bankAccountId]!!.add(model)
    }
  }
}
