package wtf.milehimikey.coffeeshop.orders

import org.javamoney.moneta.Money
import java.time.Instant

data class OrderCreated(
    val id: String,
    val customerId: String,
    val createdAt: Instant = Instant.now()
)

data class ItemAddedToOrder(
    val orderId: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val price: Money
)

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
