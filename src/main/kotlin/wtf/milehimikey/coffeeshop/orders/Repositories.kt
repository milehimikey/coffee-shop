package wtf.milehimikey.coffeeshop.orders

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant

@Document(collection = "orders")
data class OrderDocument(
    @Id val id: String,
    val customerId: String,
    val items: List<OrderItemDocument> = emptyList(),
    val status: String,
    val totalAmount: BigDecimal,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

data class OrderItemDocument(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val price: BigDecimal
)

@Repository
interface OrderRepository : MongoRepository<OrderDocument, String> {
    fun findByCustomerId(customerId: String): List<OrderDocument>
    fun findByStatus(status: String): List<OrderDocument>
}
