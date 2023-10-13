# Axon Selective Replay

[![stable](https://img.shields.io/badge/lifecycle-STABLE-green.svg)](https://github.com/holisticon#open-source-lifecycle)
[![Build Status](https://github.com/holixon/axon-selective-replay/workflows/Development%20branches/badge.svg)](https://github.com/holixon/axon-selective-replay/actions)
[![sponsored](https://img.shields.io/badge/sponsoredBy-Holisticon-RED.svg)](https://holisticon.de/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.holixon.axon.axon-selective-replay/axon-selective-replay/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.holixon.axon.axon-selective-replay/axon-axon-selective-replay)

This library provides a stateless model repository for Axon Framework using selective event replay executed during query. 
The advantage is, that one does not need a TrackingEventProcessor, TokenStore or other kind of persistence other than the Axonserver itself. 
The ModelRepository directly accesses the event store and constructs the model on the fly by applying the events directly to
the model.

## Usage

To use the extension, simply include the artifact in your POM:

```xml
    <dependency>
      <groupId>io.holixon.selectivereplay</groupId>
      <artifactId>axon-selective-replay-core</artifactId>
      <version>0.0.1</version>
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
The plugin scans for any *@MessageHandler* annotation on either constructors or methods.

**Constructors:**
- the model class must have either a default constructor or an annotated constructor accepting the first event of the event stream

**Event handlers:**
- methods which are annotated with *@MessageHandler* (or *@EventHandler* - works too) are considered to be able to handle events from the event stream
- the parameter signature is the same as for regular EventHandler methods, you can use annotations like *@SequenceNumber*, *@Timestamp* etc.
- there are only two allowed return types:
  - void: the model class will be treated as mutable class and the next event will be applied to the same instance
  - model class type: the model will be treated as immutable and the next event will be applied to the returned instance
- if an incoming event has no matching event handler, the event will simply be ignored

Then define a repository extending from *ModelRepository* class

```kotlin
class CurrentBalanceModelRepository(eventStore: EventStore) :
    ModelRepository<CurrentBalanceModel>(eventStore, CurrentBalanceModel::class.java, NoCache.INSTANCE)
```
The repository can use a cache in the same manner aggregates can be cached. By default, the *NoCache* will be used.
When using any cache implemented as in-memory solution, it is strongly advised to use an immutable model to be threadsafe.

The final usage is fairly simple:
```kotlin
val repository = CurrentBalanceModelRepository(eventStore)

val model: Optional<CurrentBalanceModel> = repository.findById(bankAccountId)
```
This method returns the model instance as *Optional* or an empty *Optional*, if the aggregate was not found. 
If a cache was specified, the current instance is also put to the cache. When retrieving cached model instances, the repository always 
compares the cached instances sequenceNumber with the lastSequenceNumber in the eventStore and applies missing events if any.

For further examples refer to the *examples* module of this repository.

## License

This library is developed under

[![Apache 2.0 License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](/LICENSE)

## Sponsors and Customers

[![sponsored](https://img.shields.io/badge/sponsoredBy-Holisticon-red.svg)](https://holisticon.de/)
