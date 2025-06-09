package wtf.milehimikey.coffeeshop.orders

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.serialization.Revision
import org.axonframework.spring.stereotype.Aggregate
import org.javamoney.moneta.Money
import java.math.BigDecimal

@Aggregate(snapshotTriggerDefinition = "orderSnapshotTriggerDefinition")
@Revision("2")
class Order {

    @AggregateIdentifier
    lateinit var id: String
    private val items: MutableList<OrderItem> = mutableListOf()
    private var status: OrderStatus = OrderStatus.NEW
    private lateinit var totalAmount: Money

    constructor() // Required by Axon

    @CommandHandler
    constructor(command: CreateOrder) {
        AggregateLifecycle.apply(
            OrderCreated(
                id = command.id,
                customerId = command.customerId
            )
        )
    }

    @CommandHandler
    fun handle(command: AddItemToOrder) {
        if (status != OrderStatus.NEW) {
            throw IllegalStateException("Cannot add items to an order that is not in NEW status")
        }

        AggregateLifecycle.apply(
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
    fun handle(command: SubmitOrder, orderTotalCalculator: OrderTotalCalculator) {
        if (status != OrderStatus.NEW) {
            throw IllegalStateException("Cannot submit an order that is not in NEW status")
        }

        if (items.isEmpty()) {
            throw IllegalStateException("Cannot submit an empty order")
        }

        AggregateLifecycle.apply(
            OrderSubmitted(
                orderId = command.orderId,
                totalAmount = orderTotalCalculator.calculateTotal(items)
            )
        )
    }

    @CommandHandler
    fun handle(command: DeliverOrder) {
        if (status != OrderStatus.SUBMITTED) {
            throw IllegalStateException("Cannot deliver an order that is not in SUBMITTED status")
        }

        AggregateLifecycle.apply(
            OrderDelivered(
                orderId = id
            )
        )
    }

    @CommandHandler
    fun handle(command: CompleteOrder) {
        if (status != OrderStatus.DELIVERED) {
            throw IllegalStateException("Cannot complete an order that is not in DELIVERED status")
        }

        AggregateLifecycle.apply(
            OrderCompleted(
                orderId = id
            )
        )
    }

    @EventSourcingHandler
    fun on(event: OrderCreated) {
        id = event.id
        status = OrderStatus.NEW
    }

    @EventSourcingHandler
    fun on(event: ItemAddedToOrder) {
        val item = OrderItem(
            productId = event.productId,
            quantity = event.quantity,
            price = event.price,
            name = event.productName
        )
        items.add(item)
    }

    @EventSourcingHandler
    fun on(event: OrderSubmitted) {
        status = OrderStatus.SUBMITTED
        totalAmount = event.totalAmount
    }

    @EventSourcingHandler
    fun on(event: OrderDelivered) {
        status = OrderStatus.DELIVERED
    }

    @EventSourcingHandler
    fun on(event: OrderCompleted) {
        status = OrderStatus.COMPLETED
    }
}

data class OrderItem(
    val productId: String,
    val quantity: Int,
    val price: BigDecimal,
    val name: String
)

enum class OrderStatus {
    NEW, SUBMITTED, DELIVERED, COMPLETED
}
