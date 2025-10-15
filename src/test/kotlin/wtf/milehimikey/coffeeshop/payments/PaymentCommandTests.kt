package wtf.milehimikey.coffeeshop.payments

import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import org.axonframework.test.matchers.Matchers.exactSequenceOf
import org.axonframework.test.matchers.Matchers.payloadsMatching
import org.axonframework.test.matchers.Matchers.predicate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class PaymentCommandTests {

    private lateinit var fixture: FixtureConfiguration<Payment>

    @BeforeEach
    fun setUp() {
        fixture = AggregateTestFixture(Payment::class.java)
    }

    @Test
    fun `should create payment`() {
        val command = CreatePayment(
            id = "payment-1",
            orderId = "order-1",
            amount = BigDecimal("42.50")
        )

        val expectedEvent = PaymentCreated(
            id = "payment-1",
            orderId = "order-1",
            amount = BigDecimal("42.50")
        )

        fixture.givenNoPriorActivity()
            .`when`(command)
            .expectSuccessfulHandlerExecution()
            .expectEvents(expectedEvent)
    }

    @Test
    fun `should process payment`() {
        val paymentId = "payment-1"
        val orderId = "order-1"
        val amount = BigDecimal("42.50")

        val processCommand = ProcessPayment(paymentId = paymentId)

        fixture.given(
            PaymentCreated(
                id = paymentId,
                orderId = orderId,
                amount = amount
            )
        )
            .`when`(processCommand)
            .expectSuccessfulHandlerExecution()
            .expectEventsMatching(payloadsMatching(exactSequenceOf(
                predicate<PaymentProcessed> { event ->
                    event.paymentId == paymentId &&
                    event.orderId == orderId &&
                    event.amount == amount &&
                    event.transactionId.isNotEmpty()
                }
            )))
    }

    @Test
    fun `should fail payment`() {
        val paymentId = "payment-1"
        val orderId = "order-1"
        val amount = BigDecimal("42.50")
        val reason = "Insufficient funds"

        val failCommand = FailPayment(
            paymentId = paymentId,
            reason = reason
        )

        fixture.given(
            PaymentCreated(
                id = paymentId,
                orderId = orderId,
                amount = amount
            )
        )
            .`when`(failCommand)
            .expectSuccessfulHandlerExecution()
            .expectEventsMatching(payloadsMatching(exactSequenceOf(
                predicate<PaymentFailed> { event ->
                    event.paymentId == paymentId &&
                    event.orderId == orderId &&
                    event.amount == amount &&
                    event.reason == reason
                }
            )))
    }

    @Test
    fun `should not fail processed payment`() {
        val paymentId = "payment-1"
        val orderId = "order-1"
        val amount = BigDecimal("42.50")

        val failCommand = FailPayment(
            paymentId = paymentId,
            reason = "Insufficient funds"
        )

        fixture.given(
            PaymentCreated(
                id = paymentId,
                orderId = orderId,
                amount = amount
            ),
            PaymentProcessed(
                paymentId = paymentId,
                orderId = orderId,
                amount = amount,
                transactionId = "tx-123",
                processedAt = Instant.now()
            )
        )
            .`when`(failCommand)
            .expectException(IllegalStateException::class.java)
    }

    @Test
    fun `should refund payment`() {
        val paymentId = "payment-1"
        val orderId = "order-1"
        val amount = BigDecimal("42.50")

        val refundCommand = RefundPayment(paymentId = paymentId)

        fixture.given(
            PaymentCreated(
                id = paymentId,
                orderId = orderId,
                amount = amount
            ),
            PaymentProcessed(
                paymentId = paymentId,
                orderId = orderId,
                amount = amount,
                transactionId = "tx-123",
                processedAt = Instant.now()
            )
        )
            .`when`(refundCommand)
            .expectSuccessfulHandlerExecution()
            .expectEventsMatching(payloadsMatching(exactSequenceOf(
                predicate<PaymentRefunded> { event ->
                    event.paymentId == paymentId &&
                    event.orderId == orderId &&
                    event.amount == amount &&
                    event.refundId.isNotEmpty()
                }
            )))
    }

    @Test
    fun `should not refund pending payment`() {
        val paymentId = "payment-1"
        val orderId = "order-1"
        val amount = BigDecimal("42.50")

        val refundCommand = RefundPayment(paymentId = paymentId)

        fixture.given(
            PaymentCreated(
                id = paymentId,
                orderId = orderId,
                amount = amount
            )
        )
            .`when`(refundCommand)
            .expectException(IllegalStateException::class.java)
    }
}
