package io.holixon.axon.projection.adhoc.rest

import io.holixon.axon.projection.adhoc.model.BankAccountCreatedEvent
import io.holixon.axon.projection.adhoc.model.MoneyDepositedEvent
import io.holixon.axon.projection.adhoc.model.MoneyWithdrawnEvent
import io.holixon.axon.projection.adhoc.model.OwnerChangedEvent
import mu.KLogging
import org.axonframework.eventhandling.DomainEventMessage
import org.axonframework.eventhandling.GenericDomainEventMessage
import org.axonframework.eventsourcing.eventstore.EventStore
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*
import kotlin.random.Random

@RestController
@RequestMapping("/data-generator")
class DataGenerationController(
  private val eventStore: EventStore
) {

  companion object : KLogging()

  @PostMapping("/")
  fun generateData(@RequestBody config: DataGenerationConfigDto): ResponseEntity<Int> {
    for (i in 1..config.numAccounts) {
      generateBankAccount(Random.nextInt(config.minInteractions, config.maxInteractions + 1))
    }

    return ResponseEntity.ok(config.numAccounts)
  }

  private fun generateBankAccount(numInteractions: Int) {
    val bankAccountId = UUID.randomUUID()

    logger.info { "Generating account $bankAccountId with $numInteractions interactions" }

    val events: MutableList<DomainEventMessage<Any>> = mutableListOf()

    events.add(
      GenericDomainEventMessage(
        BankAccountCreatedEvent::class.java.typeName,
        bankAccountId.toString(),
        0L,
        BankAccountCreatedEvent(bankAccountId, "Someone")
      )
    )

    events.add(
      GenericDomainEventMessage(
        MoneyDepositedEvent::class.java.typeName,
        bankAccountId.toString(),
        1,
        MoneyDepositedEvent(bankAccountId, Random.nextInt(5000, 7000))
      )
    )

    for (i in 2L..numInteractions) {
      if (Random.nextInt(10) == 0) {
        events.add(
          GenericDomainEventMessage(
            MoneyDepositedEvent::class.java.typeName,
            bankAccountId.toString(),
            i,
            MoneyDepositedEvent(bankAccountId, Random.nextInt(1000, 2000))
          )
        )
      } else {
        events.add(
          GenericDomainEventMessage(
            MoneyWithdrawnEvent::class.java.typeName,
            bankAccountId.toString(),
            i,
            MoneyWithdrawnEvent(bankAccountId, Random.nextInt(100, 200))
          )
        )
      }
    }

    events.add(
      GenericDomainEventMessage(
        OwnerChangedEvent::class.java.typeName,
        bankAccountId.toString(),
        events.size.toLong(),
        OwnerChangedEvent(bankAccountId, "Someone else"))
      )

    eventStore.publish(events)
  }

  data class DataGenerationConfigDto(
    val numAccounts: Int,
    val minInteractions: Int,
    val maxInteractions: Int,
  )
}
