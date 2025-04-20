package wtf.milehimikey.coffeeshop.payments

import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import org.axonframework.test.matchers.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

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

        val expectedEvent = PaymentFailed(
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
            .expectEvents(expectedEvent)
    }

    @Test
    fun `should not fail processed payment`() {
        val paymentId = "payment-1"
        val orderId = "order-1"
        val amount = BigDecimal("42.50")
        val transactionId = "tx-123"

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
                transactionId = transactionId
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
        val transactionId = "tx-123"

        val refundCommand = RefundPayment(paymentId = paymentId)

        fixture.given(
            PaymentCreated(
                id = paymentId,
                orderId = orderId,
                amount = amount
            ),
            PaymentProcessed(
                paymentId = paymentId,
                transactionId = transactionId
            )
        )
            .`when`(refundCommand)
            .expectSuccessfulHandlerExecution()
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
