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

/**
 * Compensating event representing a correction to an order item's product name.
 * This event is used to fix incorrect or null product names that were stored
 * in previous ItemAddedToOrder events.
 *
 * This demonstrates the event sourcing principle of using compensating events
 * rather than modifying historical events, maintaining a complete audit trail.
 */
data class OrderItemProductNameCorrected(
    val orderId: String,
    val productId: String,
    val oldProductName: String?,
    val correctedProductName: String,
    val correctedAt: Instant
)
