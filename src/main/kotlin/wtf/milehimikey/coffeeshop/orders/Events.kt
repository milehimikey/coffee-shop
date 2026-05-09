package wtf.milehimikey.coffeeshop.orders

import org.axonframework.eventsourcing.annotation.EventTag
import org.javamoney.moneta.Money
import java.time.Instant

data class OrderCreated(
    @EventTag(key = "orderId") val id: String,
    val customerId: String
)

data class ItemAddedToOrder(
    @EventTag(key = "orderId") val orderId: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val price: Money
)

data class OrderSubmitted(
    @EventTag(key = "orderId") val orderId: String,
    val totalAmount: Money
)

data class OrderDelivered(
    @EventTag(key = "orderId") val orderId: String,
    val customerId: String,
    val items: List<OrderItemData>,
    val totalAmount: Money,
    val deliveredAt: Instant
)

data class OrderCompleted(
    @EventTag(key = "orderId") val orderId: String,
    val customerId: String,
    val items: List<OrderItemData>,
    val totalAmount: Money,
    val deliveredAt: Instant,
    val completedAt: Instant
)

data class OrderItemData(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val price: Money
)

data class OrderItemProductNameCorrected(
    @EventTag(key = "orderId") val orderId: String,
    val productId: String,
    val oldProductName: String?,
    val correctedProductName: String,
    val correctedAt: Instant
)
