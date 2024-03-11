package io.holixon.axon.projection.adhoc.example.events;

import java.util.UUID;

public record MoneyDepositedEvent(
  String bankAccountId,
  int euroInCent) {

}
