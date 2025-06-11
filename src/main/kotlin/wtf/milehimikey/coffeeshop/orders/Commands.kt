package wtf.milehimikey.coffeeshop.orders

import org.axonframework.modelling.command.TargetAggregateIdentifier
import org.javamoney.moneta.Money
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
    val price: Money
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
