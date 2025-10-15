package wtf.milehimikey.coffeeshop.orders

import org.javamoney.moneta.Money
import java.time.Instant

data class OrderCreated(
    val id: String,
    val customerId: String
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

/**
 * Domain event representing an order being delivered.
 * Contains complete order state for proper event sourcing.
 */
data class OrderDelivered(
    val orderId: String,
    val customerId: String,
    val items: List<OrderItemData>,
    val totalAmount: Money,
    val deliveredAt: Instant
)

/**
 * Domain event representing an order being completed.
 * Contains complete order state for proper event sourcing.
 */
data class OrderCompleted(
    val orderId: String,
    val customerId: String,
    val items: List<OrderItemData>,
    val totalAmount: Money,
    val deliveredAt: Instant,
    val completedAt: Instant
)

/**
 * Data class representing order item information in events.
 * This is a simple data holder for event serialization.
 */
data class OrderItemData(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val price: Money
)
