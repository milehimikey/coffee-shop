package wtf.milehimikey.coffeeshop.orders

import org.axonframework.config.ProcessingGroup
import org.axonframework.queryhandling.QueryHandler
import org.javamoney.moneta.Money
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

// Queries
data class FindOrderById(val id: String)
data class FindOrdersByCustomerId(val customerId: String)
data class FindOrdersByStatus(val status: String)
data class FindAllOrders(val limit: Int = 100)

// Views
data class OrderView(
    val id: String,
    val customerId: String,
    val items: List<OrderItemView>,
    val status: String,
    val totalAmount: Money?,
)

data class OrderItemView(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val price: BigDecimal
)

// Query Handlers
@Component
@ProcessingGroup("order")
class OrderQueryHandler(private val orderRepository: OrderRepository) {

    @QueryHandler
    @Transactional(readOnly = true)
    fun handle(query: FindOrderById): OrderView? {
        return orderRepository.findById(query.id)
            .map { it.toView() }
            .orElse(null)
    }

    @QueryHandler
    @Transactional(readOnly = true)
    fun handle(query: FindOrdersByCustomerId): List<OrderView> {
        return orderRepository.findByCustomerId(query.customerId)
            .map { it.toView() }
    }

    @QueryHandler
    @Transactional(readOnly = true)
    fun handle(query: FindOrdersByStatus): List<OrderView> {
        return orderRepository.findByStatus(query.status)
            .map { it.toView() }
    }

    @QueryHandler
    @Transactional(readOnly = true)
    fun handle(query: FindAllOrders): List<OrderView> {
        return orderRepository.findAll()
            .take(query.limit)
            .map { it.toView() }
    }

    private fun OrderDocument.toView(): OrderView {
        return OrderView(
            id = this.id,
            customerId = this.customerId,
            items = this.items.map { it.toView() },
            status = this.status,
            totalAmount = this.totalAmount,
        )
    }

    private fun OrderItemDocument.toView(): OrderItemView {
        return OrderItemView(
            productId = this.productId,
            productName = this.productName,
            quantity = this.quantity,
            price = this.price
        )
    }
}
