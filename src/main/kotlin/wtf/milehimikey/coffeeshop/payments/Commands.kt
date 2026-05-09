package wtf.milehimikey.coffeeshop.payments

import org.axonframework.modelling.annotation.TargetEntityId
import java.math.BigDecimal
import java.util.*

data class CreatePayment(
    @TargetEntityId val id: String = UUID.randomUUID().toString(),
    val orderId: String,
    val amount: BigDecimal
)

data class ProcessPayment(
    @TargetEntityId val paymentId: String
)

data class FailPayment(
    @TargetEntityId val paymentId: String,
    val reason: String
)

data class RefundPayment(
    @TargetEntityId val paymentId: String
)

data class ResetPayment(
    @TargetEntityId val paymentId: String
)
