package io.holixon.axon.projection.adhoc.example.model;

import io.holixon.axon.projection.adhoc.LRUCache;
import io.holixon.axon.projection.adhoc.ModelRepository;
import io.holixon.axon.projection.adhoc.ModelRepositoryConfig;
import io.holixon.axon.projection.adhoc.UpdatingModelRepository;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class CurrentOwnerRepository extends UpdatingModelRepository<CurrentOwnerModel> {
  public CurrentOwnerRepository(@NotNull EventStore eventStore) {
    super(
      eventStore,
      CurrentOwnerModel.class,
      new ModelRepositoryConfig()
        .withCacheRefreshTime(10000L)
    );
  }
}
