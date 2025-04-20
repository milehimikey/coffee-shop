package wtf.milehimikey.coffeeshop.payments

import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
@ProcessingGroup("payment")
class PaymentEventProcessor(private val paymentRepository: PaymentRepository) {
    
    @EventHandler
    @Transactional
    fun on(event: PaymentCreated) {
        paymentRepository.save(
            PaymentDocument(
                id = event.id,
                orderId = event.orderId,
                amount = event.amount,
                status = PaymentStatus.PENDING.name
            )
        )
    }
    
    @EventHandler
    @Transactional
    fun on(event: PaymentProcessed) {
        paymentRepository.findById(event.paymentId).ifPresent { payment ->
            paymentRepository.save(
                payment.copy(
                    status = PaymentStatus.PROCESSED.name,
                    transactionId = event.transactionId,
                    updatedAt = Instant.now()
                )
            )
        }
    }
    
    @EventHandler
    @Transactional
    fun on(event: PaymentFailed) {
        paymentRepository.findById(event.paymentId).ifPresent { payment ->
            paymentRepository.save(
                payment.copy(
                    status = PaymentStatus.FAILED.name,
                    failureReason = event.reason,
                    updatedAt = Instant.now()
                )
            )
        }
    }
    
    @EventHandler
    @Transactional
    fun on(event: PaymentRefunded) {
        paymentRepository.findById(event.paymentId).ifPresent { payment ->
            paymentRepository.save(
                payment.copy(
                    status = PaymentStatus.REFUNDED.name,
                    refundId = event.refundId,
                    updatedAt = Instant.now()
                )
            )
        }
    }
}
