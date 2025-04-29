package wtf.milehimikey.coffeeshop.payments

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.math.BigDecimal
import java.util.*

data class CreatePayment(
    val id: String = UUID.randomUUID().toString(),
    val orderId: String,
    val amount: BigDecimal
)

data class ProcessPayment(
    @TargetAggregateIdentifier val paymentId: String
)

data class FailPayment(
    @TargetAggregateIdentifier val paymentId: String,
    val reason: String
)

data class RefundPayment(
    @TargetAggregateIdentifier val paymentId: String
)

/**
 * Special command for testing purposes to reset a payment to PENDING status.
 * This allows us to process and refund the same payment multiple times.
 */
data class ResetPayment(
    @TargetAggregateIdentifier val paymentId: String
)
