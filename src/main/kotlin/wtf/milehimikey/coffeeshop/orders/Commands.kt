package wtf.milehimikey.coffeeshop.orders

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.math.BigDecimal
import java.util.*

data class CreateOrder(
    val id: String = UUID.randomUUID().toString(),
    val customerId: String
)

data class AddItemToOrder(
    @TargetAggregateIdentifier val orderId: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val price: BigDecimal
)

data class SubmitOrder(
    @TargetAggregateIdentifier val orderId: String
)

data class DeliverOrder(
    @TargetAggregateIdentifier val orderId: String
)

data class CompleteOrder(
    @TargetAggregateIdentifier val orderId: String
)
