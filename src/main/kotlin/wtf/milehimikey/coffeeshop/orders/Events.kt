package wtf.milehimikey.coffeeshop.orders

import java.math.BigDecimal

data class OrderCreated(
    val id: String,
    val customerId: String
)

data class ItemAddedToOrder(
    val orderId: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val price: BigDecimal
)

data class OrderSubmitted(
    val orderId: String,
    val totalAmount: BigDecimal
)

data class OrderDelivered(
    val orderId: String
)

data class OrderCompleted(
    val orderId: String
)
