package wtf.milehimikey.coffeeshop.payments

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant

@Document(collection = "payments")
data class PaymentDocument(
    @Id val id: String,
    val orderId: String,
    val amount: BigDecimal,
    val status: String,
    val transactionId: String? = null,
    val refundId: String? = null,
    val failureReason: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

@Repository
interface PaymentRepository : MongoRepository<PaymentDocument, String> {
    fun findByOrderId(orderId: String): List<PaymentDocument>
    fun findByStatus(status: String): List<PaymentDocument>
}
