package wtf.milehimikey.coffeeshop.payments

import java.math.BigDecimal
import java.time.Instant

data class PaymentCreated(
    val id: String,
    val orderId: String,
    val amount: BigDecimal
)

/**
 * Domain event representing a payment being processed successfully.
 * Contains complete payment state for proper event sourcing.
 */
data class PaymentProcessed(
    val paymentId: String,
    val orderId: String,
    val amount: BigDecimal,
    val transactionId: String,
    val processedAt: Instant
)

/**
 * Domain event representing a payment failure.
 * Contains complete payment state for proper event sourcing.
 */
data class PaymentFailed(
    val paymentId: String,
    val orderId: String,
    val amount: BigDecimal,
    val reason: String,
    val failedAt: Instant
)

/**
 * Domain event representing a payment refund.
 * Contains complete payment state for proper event sourcing.
 */
data class PaymentRefunded(
    val paymentId: String,
    val orderId: String,
    val amount: BigDecimal,
    val refundId: String,
    val refundedAt: Instant
)

/**
 * Domain event emitted when a payment is reset to PENDING status.
 * This is used for testing purposes to allow processing the same payment multiple times.
 * Contains complete payment state for proper event sourcing.
 */
data class PaymentReset(
    val paymentId: String,
    val orderId: String,
    val amount: BigDecimal,
    val resetAt: Instant
)
