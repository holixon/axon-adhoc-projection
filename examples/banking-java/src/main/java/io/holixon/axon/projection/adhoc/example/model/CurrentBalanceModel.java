package io.holixon.axon.projection.adhoc.example.model;

import io.holixon.axon.projection.adhoc.example.events.BankAccountCreatedEvent;
import io.holixon.axon.projection.adhoc.example.events.MoneyDepositedEvent;
import io.holixon.axon.projection.adhoc.example.events.MoneyWithdrawnEvent;
import org.axonframework.messaging.annotation.MessageHandler;

public record CurrentBalanceModel(
  String bankAccountId,
  int currentBalanceInEuroCent
) {

  @MessageHandler
  public CurrentBalanceModel(BankAccountCreatedEvent event) {
    this(event.bankAccountId(), 0);
  }

  @MessageHandler
  public CurrentBalanceModel on(MoneyDepositedEvent event) {
    return new CurrentBalanceModel(bankAccountId, this.currentBalanceInEuroCent + event.euroInCent());
  }

  @MessageHandler
  public CurrentBalanceModel on(MoneyWithdrawnEvent event) {
    return new CurrentBalanceModel(bankAccountId, this.currentBalanceInEuroCent - event.euroInCent());
  }
}
