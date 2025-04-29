package wtf.milehimikey.coffeeshop.payments

import java.math.BigDecimal

data class PaymentCreated(
    val id: String,
    val orderId: String,
    val amount: BigDecimal
)

data class PaymentProcessed(
    val paymentId: String,
    val transactionId: String
)

data class PaymentFailed(
    val paymentId: String,
    val reason: String
)

data class PaymentRefunded(
    val paymentId: String,
    val refundId: String
)

/**
 * Event emitted when a payment is reset to PENDING status.
 * This is used for testing purposes to allow processing the same payment multiple times.
 */
data class PaymentReset(
    val paymentId: String
)
