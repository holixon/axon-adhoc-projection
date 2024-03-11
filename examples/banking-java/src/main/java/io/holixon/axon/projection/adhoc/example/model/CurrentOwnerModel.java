package io.holixon.axon.projection.adhoc.example.model;

import io.holixon.axon.projection.adhoc.example.events.BankAccountCreatedEvent;
import io.holixon.axon.projection.adhoc.example.events.MoneyDepositedEvent;
import io.holixon.axon.projection.adhoc.example.events.MoneyWithdrawnEvent;
import io.holixon.axon.projection.adhoc.example.events.OwnerChangedEvent;
import org.axonframework.messaging.annotation.MessageHandler;

public class CurrentOwnerModel {
  private final String bankAccountId;
  private String owner;

  public String getBankAccountId() {
    return bankAccountId;
  }

  public String getOwner() {
    return owner;
  }

  @MessageHandler
  public CurrentOwnerModel(BankAccountCreatedEvent event) {
    this.bankAccountId = event.bankAccountId();
    this.owner = event.owner();
  }

  @MessageHandler
  public void on(OwnerChangedEvent event) {
    this.owner = event.newOwner();
  }
}
