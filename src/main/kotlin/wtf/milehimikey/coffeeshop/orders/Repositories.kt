package wtf.milehimikey.coffeeshop.orders

import org.javamoney.moneta.Money
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Document(collection = "orders")
data class OrderDocument(
    @Id val id: String,
    val customerId: String,
    val items: List<OrderItemDocument> = emptyList(),
    var status: String,
    var totalAmount: Money? = null,
    val createdAt: Instant = Instant.now()
)

data class OrderItemDocument(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val price: Money
)

@Repository
interface OrderRepository : MongoRepository<OrderDocument, String> {
    fun findByCustomerId(customerId: String): List<OrderDocument>
    fun findByStatus(status: String): List<OrderDocument>
}
