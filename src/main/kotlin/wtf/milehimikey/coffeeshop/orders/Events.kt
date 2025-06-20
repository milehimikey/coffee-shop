package wtf.milehimikey.coffeeshop.orders

import org.axonframework.serialization.Revision
import org.javamoney.moneta.Money

data class OrderCreated(
    val id: String,
    val customerId: String
)

@Revision("2")
data class ItemAddedToOrder(
    val orderId: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val price: Money
)

@Revision("2")
data class OrderSubmitted(
    val orderId: String,
    val totalAmount: Money
)

data class OrderDelivered(
    val orderId: String
)

data class OrderCompleted(
    val orderId: String
)
