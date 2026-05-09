package wtf.milehimikey.coffeeshop.payments

import org.axonframework.conversion.DelegatingGeneralConverter
import org.axonframework.conversion.GeneralConverter
import org.axonframework.conversion.jackson2.Jackson2Converter
import org.axonframework.eventsourcing.configuration.EventSourcedEntityModule
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer
import org.axonframework.messaging.eventhandling.EventMessage
import org.axonframework.test.fixture.AxonTestFixture
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class PaymentCommandTests {

    private lateinit var fixture: AxonTestFixture

    @BeforeEach
    fun setUp() {
        fixture = AxonTestFixture.with(
            EventSourcingConfigurer.create()
                .registerEntity(EventSourcedEntityModule.autodetected(String::class.java, Payment::class.java))
                .componentRegistry { registry ->
                    registry.registerComponent(GeneralConverter::class.java) { _ -> DelegatingGeneralConverter(Jackson2Converter()) }
                }
        )
    }

    @AfterEach
    fun tearDown() {
        fixture.stop()
    }

    @Test
    fun `should create payment`() {
        val command = CreatePayment(id = "payment-1", orderId = "order-1", amount = BigDecimal("42.50"))
        val expectedEvent = PaymentCreated(id = "payment-1", orderId = "order-1", amount = BigDecimal("42.50"))

        fixture.given().noPriorActivity()
            .`when`().command(command)
            .then().success().events(expectedEvent)
    }

    @Test
    fun `should process payment`() {
        val paymentId = "payment-1"
        val orderId = "order-1"
        val amount = BigDecimal("42.50")

        fixture.given().events(
            PaymentCreated(id = paymentId, orderId = orderId, amount = amount)
        )
            .`when`().command(ProcessPayment(paymentId = paymentId))
            .then().success().eventsSatisfy { events: List<EventMessage> ->
                check(events.size == 1) { "Expected 1 event, got ${events.size}" }
                val event = events[0].payload() as PaymentProcessed
                check(event.paymentId == paymentId)
                check(event.orderId == orderId)
                check(event.amount == amount)
                check(event.transactionId.isNotEmpty())
            }
    }

    @Test
    fun `should fail payment`() {
        val paymentId = "payment-1"
        val orderId = "order-1"
        val amount = BigDecimal("42.50")
        val reason = "Insufficient funds"

        fixture.given().events(
            PaymentCreated(id = paymentId, orderId = orderId, amount = amount)
        )
            .`when`().command(FailPayment(paymentId = paymentId, reason = reason))
            .then().success().eventsSatisfy { events: List<EventMessage> ->
                check(events.size == 1) { "Expected 1 event, got ${events.size}" }
                val event = events[0].payload() as PaymentFailed
                check(event.paymentId == paymentId)
                check(event.orderId == orderId)
                check(event.amount == amount)
                check(event.reason == reason)
            }
    }

    @Test
    fun `should not fail processed payment`() {
        val paymentId = "payment-1"
        val orderId = "order-1"
        val amount = BigDecimal("42.50")

        fixture.given().events(
            PaymentCreated(id = paymentId, orderId = orderId, amount = amount),
            PaymentProcessed(
                paymentId = paymentId,
                orderId = orderId,
                amount = amount,
                transactionId = "tx-123",
                processedAt = Instant.now()
            )
        )
            .`when`().command(FailPayment(paymentId = paymentId, reason = "Insufficient funds"))
            .then().exception(IllegalStateException::class.java)
    }

    @Test
    fun `should refund payment`() {
        val paymentId = "payment-1"
        val orderId = "order-1"
        val amount = BigDecimal("42.50")

        fixture.given().events(
            PaymentCreated(id = paymentId, orderId = orderId, amount = amount),
            PaymentProcessed(
                paymentId = paymentId,
                orderId = orderId,
                amount = amount,
                transactionId = "tx-123",
                processedAt = Instant.now()
            )
        )
            .`when`().command(RefundPayment(paymentId = paymentId))
            .then().success().eventsSatisfy { events: List<EventMessage> ->
                check(events.size == 1) { "Expected 1 event, got ${events.size}" }
                val event = events[0].payload() as PaymentRefunded
                check(event.paymentId == paymentId)
                check(event.orderId == orderId)
                check(event.amount == amount)
                check(event.refundId.isNotEmpty())
            }
    }

    @Test
    fun `should not refund pending payment`() {
        val paymentId = "payment-1"
        val orderId = "order-1"
        val amount = BigDecimal("42.50")

        fixture.given().events(
            PaymentCreated(id = paymentId, orderId = orderId, amount = amount)
        )
            .`when`().command(RefundPayment(paymentId = paymentId))
            .then().exception(IllegalStateException::class.java)
    }
}
