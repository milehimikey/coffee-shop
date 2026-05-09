package wtf.milehimikey.coffeeshop.payments

import org.axonframework.eventsourcing.annotation.EventTag
import java.math.BigDecimal
import java.time.Instant

data class PaymentCreated(
    @EventTag(key = "paymentId") val id: String,
    val orderId: String,
    val amount: BigDecimal
)

data class PaymentProcessed(
    @EventTag(key = "paymentId") val paymentId: String,
    val orderId: String,
    val amount: BigDecimal,
    val transactionId: String,
    val processedAt: Instant
)

data class PaymentFailed(
    @EventTag(key = "paymentId") val paymentId: String,
    val orderId: String,
    val amount: BigDecimal,
    val reason: String,
    val failedAt: Instant
)

data class PaymentRefunded(
    @EventTag(key = "paymentId") val paymentId: String,
    val orderId: String,
    val amount: BigDecimal,
    val refundId: String,
    val refundedAt: Instant
)

data class PaymentReset(
    @EventTag(key = "paymentId") val paymentId: String,
    val orderId: String,
    val amount: BigDecimal,
    val resetAt: Instant
)
