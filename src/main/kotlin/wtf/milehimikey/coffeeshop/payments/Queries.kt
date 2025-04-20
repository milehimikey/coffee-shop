package wtf.milehimikey.coffeeshop.payments

import org.axonframework.config.ProcessingGroup
import org.axonframework.queryhandling.QueryHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

// Queries
data class FindPaymentById(val id: String)
data class FindPaymentsByOrderId(val orderId: String)
data class FindPaymentsByStatus(val status: String)
data class FindAllPayments(val limit: Int = 100)

// Views
data class PaymentView(
    val id: String,
    val orderId: String,
    val amount: BigDecimal,
    val status: String,
    val transactionId: String?,
    val refundId: String?,
    val failureReason: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

// Query Handlers
@Component
@ProcessingGroup("payment")
class PaymentQueryHandler(private val paymentRepository: PaymentRepository) {
    
    @QueryHandler
    @Transactional(readOnly = true)
    fun handle(query: FindPaymentById): PaymentView? {
        return paymentRepository.findById(query.id)
            .map { it.toView() }
            .orElse(null)
    }
    
    @QueryHandler
    @Transactional(readOnly = true)
    fun handle(query: FindPaymentsByOrderId): List<PaymentView> {
        return paymentRepository.findByOrderId(query.orderId)
            .map { it.toView() }
    }
    
    @QueryHandler
    @Transactional(readOnly = true)
    fun handle(query: FindPaymentsByStatus): List<PaymentView> {
        return paymentRepository.findByStatus(query.status)
            .map { it.toView() }
    }
    
    @QueryHandler
    @Transactional(readOnly = true)
    fun handle(query: FindAllPayments): List<PaymentView> {
        return paymentRepository.findAll()
            .take(query.limit)
            .map { it.toView() }
    }
    
    private fun PaymentDocument.toView(): PaymentView {
        return PaymentView(
            id = this.id,
            orderId = this.orderId,
            amount = this.amount,
            status = this.status,
            transactionId = this.transactionId,
            refundId = this.refundId,
            failureReason = this.failureReason,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
