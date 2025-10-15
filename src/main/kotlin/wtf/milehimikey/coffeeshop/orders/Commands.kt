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

/**
 * Command to correct an incorrect product name for an order item.
 * This is used to fix data quality issues where product names were
 * null or incorrect in the original ItemAddedToOrder events.
 */
data class CorrectOrderItemProductName(
    @TargetAggregateIdentifier val orderId: String,
    val productId: String,
    val correctedProductName: String
)
