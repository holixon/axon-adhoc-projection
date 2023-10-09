import io.holixon.selectivereplay.DuplicateHandlerException
import io.holixon.selectivereplay.IllegalReturnTypeException
import io.holixon.selectivereplay.ModelInspector
import io.holixon.selectivereplay.NoEventHandlersFoundException
import io.holixon.selectivereplay.dummy.*
import org.assertj.core.api.Assertions.assertThat
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.SequenceNumber
import org.axonframework.messaging.annotation.MessageHandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ModelInspectorTest {

  @Test
  fun `find constructor and methods of data class`() {
    val inspector = ModelInspector(CurrentBalanceImmutableModel::class.java)

    assertThat(inspector.constructors).containsOnlyKeys(BankAccountCreatedEvent::class.java)
    assertThat(inspector.methods).containsOnlyKeys(MoneyDepositedEvent::class.java, MoneyWithdrawnEvent::class.java)

    val constructorHandler = inspector.findConstructor(BankAccountCreatedEvent::class.java)
    assertThat(constructorHandler).isNotNull()
    assertThat(constructorHandler!!.payloadType()).isEqualTo(BankAccountCreatedEvent::class.java)
    assertThat(inspector.findConstructor(MoneyDepositedEvent::class.java)).isNull()

    val methodHandler = inspector.findEventHandler(MoneyDepositedEvent::class.java)
    assertThat(methodHandler).isNotNull()
    assertThat(methodHandler!!.payloadType()).isEqualTo(MoneyDepositedEvent::class.java)
    assertThat(inspector.findEventHandler(BankAccountCreatedEvent::class.java)).isNull()
  }

  @Test
  fun `find constructor and methods of regular class`() {
    val inspector = ModelInspector(CurrentBalanceMutableModel::class.java)

    assertThat(inspector.constructors).isEmpty()
    assertThat(inspector.getDefaultConstructor()) // does not throw
    assertThat(inspector.methods).containsOnlyKeys(
      BankAccountCreatedEvent::class.java,
      MoneyDepositedEvent::class.java,
      MoneyWithdrawnEvent::class.java
    )
  }

  @Test
  fun `throws on empty class`() {
    assertThrows<NoEventHandlersFoundException> {
      ModelInspector(NoHandlersModel::class.java)
    }
  }

  @Test
  fun `throws on duplicate constructor`() {
    assertThrows<DuplicateHandlerException> {
      ModelInspector(DuplicateConstructorModel::class.java)
    }
  }

  @Test
  fun `throws on duplicate handler`() {
    assertThrows<DuplicateHandlerException> {
      ModelInspector(DuplicateEventHandlerModel::class.java)
    }
  }

  @Test
  fun `throws on illegal return type`() {
    assertThrows<IllegalReturnTypeException> {
      ModelInspector(IllegalReturnTypeModel::class.java)
    }
  }

}

class NoHandlersModel {
  fun foo() {}
}

class DuplicateConstructorModel {
  @MessageHandler
  constructor(evt: BankAccountCreatedEvent)

  @MessageHandler
  constructor(evt: BankAccountCreatedEvent, @SequenceNumber version: Long)
}

class DuplicateEventHandlerModel {
  @EventHandler
  fun on(evt: BankAccountCreatedEvent) {
  }

  @EventHandler
  fun on(evt: BankAccountCreatedEvent, @SequenceNumber version: Long) {
  }
}

class IllegalReturnTypeModel {
  @EventHandler
  fun on(evt: BankAccountCreatedEvent): Long {
    return 0L
  }
}
