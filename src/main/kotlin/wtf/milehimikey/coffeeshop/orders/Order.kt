package wtf.milehimikey.coffeeshop.orders

import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.javamoney.moneta.Money
import java.time.Instant

@EventSourced(tagKey = "orderId")
class Order {

    private lateinit var id: String
    private lateinit var customerId: String
    private val items: MutableList<OrderItem> = mutableListOf()
    private var status: OrderStatus = OrderStatus.NEW
    private lateinit var totalAmount: Money
    private var deliveredAt: Instant? = null

    companion object {
        @JvmStatic
        @CommandHandler
        fun create(command: CreateOrder, appender: EventAppender) {
            appender.append(OrderCreated(id = command.id, customerId = command.customerId))
        }
    }

    @EntityCreator
    constructor(event: OrderCreated) {
        id = event.id
        customerId = event.customerId
        status = OrderStatus.NEW
    }

    @CommandHandler
    fun handle(command: AddItemToOrder, appender: EventAppender) {
        if (status != OrderStatus.NEW) {
            throw IllegalStateException("Cannot add items to an order that is not in NEW status")
        }
        appender.append(
            ItemAddedToOrder(
                orderId = id,
                productId = command.productId,
                quantity = command.quantity,
                price = command.price,
                productName = command.productName
            )
        )
    }

    @CommandHandler
    fun handle(command: SubmitOrder, appender: EventAppender, orderTotalCalculator: OrderTotalCalculator) {
        if (status != OrderStatus.NEW) {
            throw IllegalStateException("Cannot submit an order that is not in NEW status")
        }
        if (items.isEmpty()) {
            throw IllegalStateException("Cannot submit an empty order")
        }
        appender.append(
            OrderSubmitted(
                orderId = command.orderId,
                totalAmount = orderTotalCalculator.calculateTotal(items)
            )
        )
    }

    @CommandHandler
    fun handle(command: DeliverOrder, appender: EventAppender) {
        if (status != OrderStatus.SUBMITTED) {
            throw IllegalStateException("Cannot deliver an order that is not in SUBMITTED status")
        }
        appender.append(
            OrderDelivered(
                orderId = id,
                customerId = customerId,
                items = items.map { it.toOrderItemData() },
                totalAmount = totalAmount,
                deliveredAt = Instant.now()
            )
        )
    }

    @CommandHandler
    fun handle(command: CompleteOrder, appender: EventAppender) {
        if (status != OrderStatus.DELIVERED) {
            throw IllegalStateException("Cannot complete an order that is not in DELIVERED status")
        }
        appender.append(
            OrderCompleted(
                orderId = id,
                customerId = customerId,
                items = items.map { it.toOrderItemData() },
                totalAmount = totalAmount,
                deliveredAt = deliveredAt!!,
                completedAt = Instant.now()
            )
        )
    }

    @CommandHandler
    fun handle(command: CorrectOrderItemProductName, appender: EventAppender) {
        val item = items.find { it.productId == command.productId }
            ?: throw IllegalArgumentException("Order item with productId ${command.productId} not found in order $id")
        if (command.correctedProductName.isBlank()) {
            throw IllegalArgumentException("Corrected product name cannot be blank")
        }
        appender.append(
            OrderItemProductNameCorrected(
                orderId = id,
                productId = command.productId,
                oldProductName = item.name,
                correctedProductName = command.correctedProductName,
                correctedAt = Instant.now()
            )
        )
    }

    @EventSourcingHandler
    fun on(event: ItemAddedToOrder) {
        items.add(
            OrderItem(
                productId = event.productId,
                quantity = event.quantity,
                price = event.price,
                name = event.productName
            )
        )
    }

    @EventSourcingHandler
    fun on(event: OrderSubmitted) {
        status = OrderStatus.SUBMITTED
        totalAmount = event.totalAmount
    }

    @EventSourcingHandler
    fun on(event: OrderDelivered) {
        status = OrderStatus.DELIVERED
        deliveredAt = event.deliveredAt
    }

    @EventSourcingHandler
    fun on(event: OrderCompleted) {
        status = OrderStatus.COMPLETED
    }

    @EventSourcingHandler
    fun on(event: OrderItemProductNameCorrected) {
        val item = items.find { it.productId == event.productId }
        item?.let { it.name = event.correctedProductName }
    }
}

data class OrderItem(
    val productId: String,
    val quantity: Int,
    val price: Money,
    var name: String
) {
    fun toOrderItemData(): OrderItemData {
        return OrderItemData(
            productId = productId,
            productName = name,
            quantity = quantity,
            price = price
        )
    }
}

enum class OrderStatus {
    NEW, SUBMITTED, DELIVERED, COMPLETED
}
