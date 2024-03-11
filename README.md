# Axon Adhoc Projection

[![stable](https://img.shields.io/badge/lifecycle-STABLE-green.svg)](https://github.com/holisticon#open-source-lifecycle)
[![Build Status](https://github.com/holixon/axon-adhoc-projection/workflows/Development%20branches/badge.svg)](https://github.com/holixon/axon-adhoc-projection/actions)
[![sponsored](https://img.shields.io/badge/sponsoredBy-Holisticon-RED.svg)](https://holisticon.de/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.holixon.axon/axon-adhoc-projection-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.holixon.axon/axon-adhoc-projection-core)

This library provides a stateless model repository for a single aggregate for the Axon Framework using selective event
retrieval that is performed during the query.
The advantage is, that you do not need a `TrackingEventProcessor`, `TokenStore`  or any other kind of persistence other
than the Axon Server itself.
The `ModelRepository` accesses the event store directly and constructs the model on the fly by applying the events
directly to the model.

> **_NOTE:_**  This library is still under development. API changes may occur while using versions 0.x

## Usage

To use the extension, simply include the artifact in your POM:

```xml

<dependency>
    <groupId>io.holixon.axon</groupId>
    <artifactId>axon-adhoc-projection-core</artifactId>
    <version>0.0.2</version>
</dependency>
```

Then define a model class:

```kotlin
data class CurrentBalanceModel(
  val bankAccountId: UUID,
  val currentBalanceInEuroCent: Int,
  val lastModification: Instant,
  val version: Long
) {

  @MessageHandler
  constructor(evt: BankAccountCreatedEvent, @Timestamp messageTimestamp: Instant, @SequenceNumber version: Long) : this(
    bankAccountId = evt.bankAccountId,
    currentBalanceInEuroCent = 0,
    lastModification = messageTimestamp,
    version = version,
  )

  @MessageHandler
  fun on(
    evt: MoneyDepositedEvent,
    @Timestamp messageTimestamp: Instant,
    @SequenceNumber version: Long
  ): CurrentBalanceModel = copy(
    currentBalanceInEuroCent = this.currentBalanceInEuroCent + evt.amountInEuroCent,
    lastModification = messageTimestamp,
    version = version,
  )
}
```

The plugin scans for any `@MessageHandler` annotation on either constructors or methods.

**Constructors:**

- The model class must have either a default constructor or an annotated constructor that accepts the first event of the
  event stream.

**Event handlers:**

- Methods which are annotated with `@MessageHandler` (or `@EventHandler` - works too) are considered to be able to
  handle events from the event stream.
- The parameter signature is the same as for regular EventHandler methods, you can use annotations
  like `@SequenceNumber`, `@Timestamp` etc.
- There are only two allowed return types:
    - Void: the model class will be treated as mutable class and the next event will be applied to the same instance.
    - Model class type: the model will be treated as immutable and the next event will be applied to the returned
      instance.
- If an incoming event has no matching event handler, the event will simply be ignored.

Then define a repository that extends the `ModelRepository` class

```kotlin
class CurrentBalanceModelRepository(eventStore: EventStore) :
  ModelRepository<CurrentBalanceModel>(eventStore, CurrentBalanceModel::class.java)
```

The repository can use a cache in the same way that aggregates can be cached. By default, the `NoCache` is used.
When using a cache implemented as in-memory solution, it is strongly recommended to use an immutable model to be
thread-safe.

Normally the repository will start reading events from the latest snapshot event onwards (if snapshotting is enabled).
If you always want to read the events from the beginning, you can enable this via the
boolean option `ignoreSnapshotEvents` in the `ModelRepository` class.

```kotlin
class CurrentBalanceModelRepository(eventStore: EventStore) :
  ModelRepository<CurrentBalanceModel>(
    eventStore,
    CurrentBalanceModel::class.java,
    NoCache.INSTANCE,
    ignoreSnapshotEvents = true
  )
```

The final usage is fairly simple:

```kotlin
val repository = CurrentBalanceModelRepository(eventStore)

val model: Optional<CurrentBalanceModel> = repository.findById(bankAccountId)
```

This method returns the model instance as `Optional` or an empty `Optional`, if the aggregate was not found.
If a cache was specified, the current instance is also put in the cache. When fetching cached model instances, the
repository always
compares the sequenceNumber of the cached instance with the lastSequenceNumber in the eventStore and applies any missing
events.

For more examples, see to the *examples* module in this repository.

### Self-updating cache projection

As an extension to the ModelRepository, the `UpdatingModelRepository` is able to update the underlying cache as new
events arrive for cached model entities.

```kotlin
@Component
class CurrentBalanceModelRepository(eventStore: EventStore) :
  UpdatingModelRepository<CurrentBalanceModel>(eventStore, CurrentBalanceModel::class.java) {
  companion object : KLogging()

  init {
    /**
     * Add a listener which fires on every model change. Can be used to trigger query subscriptions.
     */
    addModelUpdatedListener { logger.debug { "Updated ${it.bankAccountId}" } }
  }
}
```

Currently, the easiest way to use the `UpdatingModelListener` is to use the Spring module of this plugin. This uses an
auto-configuration to register all
found beans of type `UpdatingModelRepository` as Axon EventHandler.

```xml

<dependency>
    <groupId>io.holixon.axon</groupId>
    <artifactId>axon-adhoc-projection-spring</artifactId>
    <version>0.0.2</version>
</dependency>
```

All UpdatingModelRepositories use the same Axon processing group `adhoc-event-message-handler`. This processing group
can
be configured in the same way as any other processing group.

Since all repositories are processed in the same processing group, an error in processing an event in one of the
repositories lead to a
failure of the whole event for the processor so the event may be dead-lettered or retried with a backoff (depending on
the configured behavior of the processingGroup).
However, the event will still be forwarded to _all_ repositories, even if one threw an error. Since the cache entry also
holds the seqNo of the last processed
event for each repository, an event will not be processed twice by any repository.

The event processing itself is not further configured, so by default a TrackingEventProcessor is created using a tail
token. It is strongly recommended to add a configuration
to use a head token so that the entire eventStore is not replayed:

```kotlin
@Bean
fun adhocProjectionConfigurerModule(): ConfigurerModule? {
  return ConfigurerModule { configurer: Configurer ->
    configurer.eventProcessing { processingConfigurer: EventProcessingConfigurer ->
      processingConfigurer.registerTrackingEventProcessorConfiguration(AdhocEventMessageHandler.PROCESSING_GROUP) {
        TrackingEventProcessorConfiguration.forParallelProcessing(4).andInitialTrackingToken { it.createHeadToken() }
      }
    }
  }
}
```

## Configuration

The `ModelRepository` and subclasses take a `ModelRepositoryConfig` object for more detailed configuration.

| Parameter            | Description                                                                                                                                                                                                                                     | Default value  |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------|
| cache                | the cache implementation to use                                                                                                                                                                                                                 | LRUCache(1024) |
| cacheRefreshTime     | Time in ms a cache entry is considered up-to-date and no eventStore will be queried for new/missed events.<br/>When using the UpdatingModelRepository consider a value other than 0 to use the advantage of the self-updating repo.             | 0 (ms)         |
| forceCacheInsert     | Just for UpdatingModelRepository - Configures the behavior when an event of an uncached entity is received.<br/>When _false_ the event is ignored, when _true_, a full replay of this entity is performed and the result is added to the cache. | false          |
| ignoreSnapshotEvents | By default, a DomainEventStream for an aggregate starts at the last snapshot event and ignores all prior events. With this flag set to <code>true</code>, the stream will always start at sequenceNo 0.                                         | false          |

## License

This library is developed under

[![Apache 2.0 License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](/LICENSE)

## Sponsors and Customers

[![sponsored](https://img.shields.io/badge/sponsoredBy-Holisticon-red.svg)](https://holisticon.de/)
