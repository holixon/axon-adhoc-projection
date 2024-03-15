package io.holixon.axon.projection.adhoc.example.model;

import io.holixon.axon.projection.adhoc.ModelRepository;
import io.holixon.axon.projection.adhoc.ModelRepositoryConfig;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class CurrentBalanceRepository extends ModelRepository<CurrentBalanceModel> {
  public CurrentBalanceRepository(EventStore eventStore) {
    super(eventStore, CurrentBalanceModel.class, new ModelRepositoryConfig());
  }
}
