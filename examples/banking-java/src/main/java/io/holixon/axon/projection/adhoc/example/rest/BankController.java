package io.holixon.axon.projection.adhoc.example.rest;

import io.holixon.axon.projection.adhoc.example.events.BankAccountCreatedEvent;
import io.holixon.axon.projection.adhoc.example.events.MoneyDepositedEvent;
import io.holixon.axon.projection.adhoc.example.events.MoneyWithdrawnEvent;
import io.holixon.axon.projection.adhoc.example.events.OwnerChangedEvent;
import io.holixon.axon.projection.adhoc.example.model.CurrentBalanceModel;
import io.holixon.axon.projection.adhoc.example.model.CurrentBalanceRepository;
import io.holixon.axon.projection.adhoc.example.model.CurrentOwnerModel;
import io.holixon.axon.projection.adhoc.example.model.CurrentOwnerRepository;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/bankaccount")
public class BankController {

  private final CurrentBalanceRepository currentBalanceRepository;
  private final CurrentOwnerRepository currentOwnerRepository;
  private final EventStore eventStore;

  public BankController(CurrentBalanceRepository currentBalanceRepository, CurrentOwnerRepository currentOwnerRepository, EventStore eventStore) {
    this.currentBalanceRepository = currentBalanceRepository;
    this.currentOwnerRepository = currentOwnerRepository;
    this.eventStore = eventStore;
  }

  @GetMapping("/{bankAccountId}/balance")
  public ResponseEntity<CurrentBalanceModel> getBalance(@PathVariable("bankAccountId") String bankAccountId) {
    return this.currentBalanceRepository.findById(bankAccountId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/{bankAccountId}/owner")
  public ResponseEntity<CurrentOwnerModel> getOwnerBalance(@PathVariable("bankAccountId") String bankAccountId) {
    return this.currentOwnerRepository.findById(bankAccountId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @PutMapping
  public ResponseEntity<String> createAccount(@RequestParam("owner") String owner) {
    final String bankAccountId = UUID.randomUUID().toString();
    final BankAccountCreatedEvent bankAccountCreatedEvent = new BankAccountCreatedEvent(bankAccountId, owner);

    eventStore.publish(new GenericDomainEventMessage<>(
        BankAccountCreatedEvent.class.getTypeName(),
        bankAccountId,
        0L,
        bankAccountCreatedEvent
      )
    );

    return ResponseEntity.ok(bankAccountId);
  }

  @PostMapping("/{bankAccountId}/deposit")
  public ResponseEntity<Void> depositMoney(@PathVariable("bankAccountId") String bankAccountId, @RequestParam("euroInCent") int amount) {
    final MoneyDepositedEvent event = new MoneyDepositedEvent(bankAccountId, amount);

    final long lastSeqNo = eventStore.lastSequenceNumberFor(bankAccountId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    eventStore.publish(new GenericDomainEventMessage<>(
        MoneyDepositedEvent.class.getTypeName(),
        bankAccountId,
        lastSeqNo + 1,
        event
      )
    );

    return ResponseEntity.ok().build();
  }

  @PostMapping("/{bankAccountId}/withdraw")
  public ResponseEntity<Void> withdrawMoney(@PathVariable("bankAccountId") String bankAccountId, @RequestParam("euroInCent") int amount) {
    final MoneyWithdrawnEvent event = new MoneyWithdrawnEvent(bankAccountId, amount);

    final long lastSeqNo = eventStore.lastSequenceNumberFor(bankAccountId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    eventStore.publish(new GenericDomainEventMessage<>(
        MoneyWithdrawnEvent.class.getTypeName(),
        bankAccountId,
        lastSeqNo + 1,
        event
      )
    );

    return ResponseEntity.ok().build();
  }

  @PostMapping("/{bankAccountId}/change-owner")
  public ResponseEntity<Void> changeOwner(@PathVariable("bankAccountId") String bankAccountId, @RequestParam("newOwner") String newOwner) {
    final OwnerChangedEvent event = new OwnerChangedEvent(bankAccountId, newOwner);

    final long lastSeqNo = eventStore.lastSequenceNumberFor(bankAccountId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    eventStore.publish(new GenericDomainEventMessage<>(
      OwnerChangedEvent.class.getTypeName(),
        bankAccountId,
        lastSeqNo + 1,
        event
      )
    );

    return ResponseEntity.ok().build();
  }
}
