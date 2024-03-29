package io.holixon.axon.projection.adhoc

import io.holixon.axon.projection.adhoc._itestbase.BaseSpringIntegrationTest
import io.holixon.axon.projection.adhoc.dummy.*
import org.assertj.core.api.Assertions
import org.axonframework.eventhandling.GenericDomainEventMessage
import org.axonframework.eventsourcing.eventstore.EventStore

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.*

class ModelRepositoryITest : BaseSpringIntegrationTest() {

  @Test
  fun `immutable projection can be loaded`() {
    val bankAccountId = UUID.randomUUID()
    publishMessage(bankAccountId.toString(), BankAccountCreatedEvent(bankAccountId, ""))
    publishMessage(bankAccountId.toString(), MoneyDepositedEvent(bankAccountId, 100))
    publishMessage(bankAccountId.toString(), MoneyWithdrawnEvent(bankAccountId, 50))

    val repository = CurrentBalanceImmutableModelRepository(eventStore)

    val model = repository.findById(bankAccountId.toString())

    Assertions.assertThat(model).isPresent
    Assertions.assertThat(model.get().currentBalanceInEuroCent).isEqualTo(50)
    Assertions.assertThat(model.get().version).isEqualTo(2)
  }

  @Test
  fun `mutable projection can be loaded`() {
    val bankAccountId = UUID.randomUUID()
    publishMessage(bankAccountId.toString(), BankAccountCreatedEvent(bankAccountId, ""))
    publishMessage(bankAccountId.toString(), MoneyDepositedEvent(bankAccountId, 100))
    publishMessage(bankAccountId.toString(), MoneyWithdrawnEvent(bankAccountId, 50))

    val repository = CurrentBalanceMutableModelRepository(eventStore)

    val model = repository.findById(bankAccountId.toString())

    Assertions.assertThat(model).isPresent
    Assertions.assertThat(model.get().currentBalanceInEuroCent).isEqualTo(50)
    Assertions.assertThat(model.get().version).isEqualTo(2)

    publishMessage(bankAccountId.toString(), MoneyDepositedEvent(bankAccountId, 120))
    publishMessage(bankAccountId.toString(), MoneyWithdrawnEvent(bankAccountId, 60))

    val secondModel = repository.findById(bankAccountId.toString())

    Assertions.assertThat(secondModel).isPresent
    Assertions.assertThat(secondModel.get().currentBalanceInEuroCent).isEqualTo(110)
    Assertions.assertThat(secondModel.get().version).isEqualTo(4)
  }
}
