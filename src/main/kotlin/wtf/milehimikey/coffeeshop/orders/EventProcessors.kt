package wtf.milehimikey.coffeeshop.orders

import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

@Component
@ProcessingGroup("order")
class OrderEventProcessor(private val orderRepository: OrderRepository) {

    private val logger = LoggerFactory.getLogger(OrderEventProcessor::class.java)

    /**
     * Special customer ID that will trigger an error in the event processor.
     * Used to demonstrate dead letter queue functionality.
     */
    private val ERROR_TRIGGERING_CUSTOMER_ID = "error-customer"

    @EventHandler
    fun on(event: OrderCreated) {
        logger.info("Processing OrderCreated event for order ${event.id}")

        // Check if this order should fail
        if (shouldFailOrderProcessing(event.customerId)) {
            logger.error("Simulated error processing OrderCreated event for order ${event.id} with customer ID $ERROR_TRIGGERING_CUSTOMER_ID")
            throw RuntimeException("Simulated error processing OrderCreated event for order with customer ID $ERROR_TRIGGERING_CUSTOMER_ID")
        }

        orderRepository.save(
            OrderDocument(
                id = event.id,
                customerId = event.customerId,
                items = emptyList(),
                status = OrderStatus.NEW.name,
                totalAmount = BigDecimal.ZERO
            )
        )
    }

    @EventHandler
    fun on(event: ItemAddedToOrder) {
        logger.info("Processing ItemAddedToOrder event for order ${event.orderId}")

        orderRepository.findById(event.orderId).ifPresent { order ->
            // Check if this order should fail
            if (shouldFailOrderProcessing(order.customerId)) {
                logger.error("Simulated error processing ItemAddedToOrder event for order ${event.orderId} with customer ID $ERROR_TRIGGERING_CUSTOMER_ID")
                throw RuntimeException("Simulated error processing ItemAddedToOrder event for order with customer ID $ERROR_TRIGGERING_CUSTOMER_ID")
            }

            val updatedItems = order.items.toMutableList().apply {
                add(
                    OrderItemDocument(
                        productId = event.productId,
                        productName = event.productName,
                        quantity = event.quantity,
                        price = event.price
                    )
                )
            }

            val newTotalAmount = updatedItems.fold(BigDecimal.ZERO) { acc, item ->
                acc.add(item.price.multiply(BigDecimal(item.quantity)))
            }

            orderRepository.save(
                order.copy(
                    items = updatedItems,
                    totalAmount = newTotalAmount,
                    updatedAt = Instant.now()
                )
            )
        }
    }

    @EventHandler
    fun on(event: OrderSubmitted) {
        logger.info("Processing OrderSubmitted event for order ${event.orderId}")

        orderRepository.findById(event.orderId).ifPresent { order ->
            // Check if this order should fail
            if (shouldFailOrderProcessing(order.customerId)) {
                logger.error("Simulated error processing OrderSubmitted event for order ${event.orderId} with customer ID $ERROR_TRIGGERING_CUSTOMER_ID")
                throw RuntimeException("Simulated error processing OrderSubmitted event for order with customer ID $ERROR_TRIGGERING_CUSTOMER_ID")
            }

            orderRepository.save(
                order.copy(
                    status = OrderStatus.SUBMITTED.name,
                    updatedAt = Instant.now()
                )
            )
        }
    }

    @EventHandler
    fun on(event: OrderDelivered) {
        logger.info("Processing OrderDelivered event for order ${event.orderId}")

        orderRepository.findById(event.orderId).ifPresent { order ->
            orderRepository.save(
                order.copy(
                    status = OrderStatus.DELIVERED.name,
                    updatedAt = Instant.now()
                )
            )
        }
    }

    @EventHandler
    fun on(event: OrderCompleted) {
        logger.info("Processing OrderCompleted event for order ${event.orderId}")

        orderRepository.findById(event.orderId).ifPresent { order ->
            orderRepository.save(
                order.copy(
                    status = OrderStatus.COMPLETED.name,
                    updatedAt = Instant.now()
                )
            )
        }
    }

    /**
     * Helper method to determine if an order should fail processing based on its customer ID.
     */
    private fun shouldFailOrderProcessing(customerId: String): Boolean {
        return customerId == ERROR_TRIGGERING_CUSTOMER_ID
    }
}
