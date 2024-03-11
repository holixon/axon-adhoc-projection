package io.holixon.axon.projection.adhoc.example.rest

import io.holixon.axon.projection.adhoc.example.model.*
import org.axonframework.eventhandling.GenericDomainEventMessage
import org.axonframework.eventsourcing.eventstore.EventStore
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

@RestController
@RequestMapping("/bankaccount")
class BankController(
  val currentBalanceModelRepository: CurrentBalanceModelRepository,
  val currentOwnerModelRepository: CurrentOwnerModelRepository,
  val eventStore: EventStore,
) {
  @GetMapping("/{bankAccountId}/balance")
  fun getBalance(@PathVariable("bankAccountId") bankAccountId: String): ResponseEntity<CurrentBalanceModel> {
    currentBalanceModelRepository.findById(bankAccountId).orElse(null)?.let {
      return ResponseEntity.ok(it)
    }

    return ResponseEntity.notFound().build()
  }

  @GetMapping("/{bankAccountId}/owner")
  fun getOwner(@PathVariable("bankAccountId") bankAccountId: String): ResponseEntity<CurrentOwnerModel> {
    currentOwnerModelRepository.findById(bankAccountId).orElse(null)?.let {
      return ResponseEntity.ok(it)
    }

    return ResponseEntity.notFound().build()
  }

  @PutMapping
  fun createAccount(@RequestParam("owner") owner: String): ResponseEntity<String> {
    val bankAccountId = UUID.randomUUID()
    eventStore.publish(
      GenericDomainEventMessage(
        BankAccountCreatedEvent::class.java.getTypeName(),
        bankAccountId.toString(),
        0L,
        BankAccountCreatedEvent(bankAccountId, owner)
      )
    )
    return ResponseEntity.ok(bankAccountId.toString())
  }

  @PostMapping("/{bankAccountId}/deposit")
  fun depositMoney(@PathVariable("bankAccountId") bankAccountId: UUID, @RequestParam("euroInCent") amount: Int): ResponseEntity<Void> {
    val lastSeqNo = eventStore.lastSequenceNumberFor(bankAccountId.toString()).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }

    eventStore.publish(
      GenericDomainEventMessage(
        MoneyDepositedEvent::class.java.getTypeName(),
        bankAccountId.toString(),
        lastSeqNo + 1,
        MoneyDepositedEvent(bankAccountId, amount)
      )
    )
    return ResponseEntity.ok().build()
  }

  @PostMapping("/{bankAccountId}/withdraw")
  fun withdrawMoney(@PathVariable("bankAccountId") bankAccountId: UUID, @RequestParam("euroInCent") amount: Int): ResponseEntity<Void> {
    val lastSeqNo = eventStore.lastSequenceNumberFor(bankAccountId.toString()).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }

    eventStore.publish(
      GenericDomainEventMessage(
        MoneyWithdrawnEvent::class.java.getTypeName(),
        bankAccountId.toString(),
        lastSeqNo + 1,
        MoneyWithdrawnEvent(bankAccountId, amount)
      )
    )
    return ResponseEntity.ok().build()
  }

  @PostMapping("/{bankAccountId}/change-owner")
  fun changeOwner(@PathVariable("bankAccountId") bankAccountId: UUID, @RequestParam("newOwner") newOwner: String): ResponseEntity<Void> {
    val lastSeqNo = eventStore.lastSequenceNumberFor(bankAccountId.toString()).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }

    eventStore.publish(
      GenericDomainEventMessage(
        OwnerChangedEvent::class.java.getTypeName(),
        bankAccountId.toString(),
        lastSeqNo + 1,
        OwnerChangedEvent(bankAccountId, newOwner)
      )
    )
    return ResponseEntity.ok().build()
  }


}
