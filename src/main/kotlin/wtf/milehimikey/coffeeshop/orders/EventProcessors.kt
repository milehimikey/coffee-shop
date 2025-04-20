package wtf.milehimikey.coffeeshop.orders

import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

@Component
@ProcessingGroup("order")
class OrderEventProcessor(private val orderRepository: OrderRepository) {
    
    @EventHandler
    @Transactional
    fun on(event: OrderCreated) {
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
    @Transactional
    fun on(event: ItemAddedToOrder) {
        orderRepository.findById(event.orderId).ifPresent { order ->
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
    @Transactional
    fun on(event: OrderSubmitted) {
        orderRepository.findById(event.orderId).ifPresent { order ->
            orderRepository.save(
                order.copy(
                    status = OrderStatus.SUBMITTED.name,
                    updatedAt = Instant.now()
                )
            )
        }
    }
    
    @EventHandler
    @Transactional
    fun on(event: OrderDelivered) {
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
    @Transactional
    fun on(event: OrderCompleted) {
        orderRepository.findById(event.orderId).ifPresent { order ->
            orderRepository.save(
                order.copy(
                    status = OrderStatus.COMPLETED.name,
                    updatedAt = Instant.now()
                )
            )
        }
    }
}
