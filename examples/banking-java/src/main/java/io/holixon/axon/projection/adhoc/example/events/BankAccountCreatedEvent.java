package io.holixon.axon.projection.adhoc.example.events;

import java.util.UUID;

public record BankAccountCreatedEvent(
  String bankAccountId,
  String owner
) {

}
