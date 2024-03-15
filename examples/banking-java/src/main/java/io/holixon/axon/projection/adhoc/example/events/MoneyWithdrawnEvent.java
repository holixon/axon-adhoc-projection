package io.holixon.axon.projection.adhoc.example.events;

import java.util.UUID;

public record MoneyWithdrawnEvent(
  String bankAccountId,
  int euroInCent) {

}
