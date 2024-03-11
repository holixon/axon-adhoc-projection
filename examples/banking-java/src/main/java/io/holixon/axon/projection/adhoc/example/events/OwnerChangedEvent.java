package io.holixon.axon.projection.adhoc.example.events;

import java.util.UUID;

public record OwnerChangedEvent(
  String bankAccountId,
  String newOwner) {

}
