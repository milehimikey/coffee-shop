package wtf.milehimikey.coffeeshop.payments

import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.ResetHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@ProcessingGroup("payment")
class PaymentEventProcessor(private val paymentRepository: PaymentRepository) {

    private val logger = LoggerFactory.getLogger(PaymentEventProcessor::class.java)

    @EventHandler
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
    fun on(event: PaymentProcessed) {
        // Event now contains all necessary data - no repository lookup needed
        paymentRepository.findById(event.paymentId).ifPresent { payment ->
            paymentRepository.save(
                payment.copy(
                    status = PaymentStatus.PROCESSED.name,
                    transactionId = event.transactionId,
                    updatedAt = event.processedAt
                )
            )
        }
    }

    @EventHandler
    fun on(event: PaymentFailed) {
        // Event now contains all necessary data - no repository lookup needed
        paymentRepository.findById(event.paymentId).ifPresent { payment ->
            paymentRepository.save(
                payment.copy(
                    status = PaymentStatus.FAILED.name,
                    failureReason = event.reason,
                    updatedAt = event.failedAt
                )
            )
        }
    }

    @EventHandler
    fun on(event: PaymentRefunded) {
        // Event now contains all necessary data - no repository lookup needed
        paymentRepository.findById(event.paymentId).ifPresent { payment ->
            paymentRepository.save(
                payment.copy(
                    status = PaymentStatus.REFUNDED.name,
                    refundId = event.refundId,
                    updatedAt = event.refundedAt
                )
            )
        }
    }

    /**
     * Handler for PaymentReset events that intentionally throws an exception
     * when the payment amount is exactly $13.13 to demonstrate dead letter queue functionality.
     * Event now contains all necessary data - no repository lookup needed for amount check.
     */
    @EventHandler
    fun on(event: PaymentReset) {
        logger.info("Processing PaymentReset event for payment ${event.paymentId}")

        // Check if this payment should fail using event data (no repository lookup needed)
        if (isErrorTriggeringAmount(event.amount)) {
            logger.error("Simulated error processing PaymentReset event for payment ${event.paymentId} with amount $13.13")
            throw RuntimeException("Simulated error processing PaymentReset event for payment with amount $13.13")
        }

        val payment = paymentRepository.findById(event.paymentId)
        if (payment.isPresent) {
            logger.info("Resetting payment ${event.paymentId} to PENDING status")
            paymentRepository.save(
                payment.get().copy(
                    status = PaymentStatus.PENDING.name,
                    transactionId = null,
                    refundId = null,
                    failureReason = null,
                    updatedAt = event.resetAt
                )
            )
        }
    }

    /**
     * Helper method to check if a payment has the error-triggering amount
     * Used by tests and other components to identify payments that will cause errors
     */
    fun isErrorTriggeringAmount(amount: java.math.BigDecimal): Boolean {
        return amount.compareTo(java.math.BigDecimal("13.13")) == 0
    }

    /**
     * Reset handler that is called when the event processor is reset.
     * This clears the read model for this processor.
     */
    @ResetHandler
    fun reset() {
        logger.info("Resetting payment event processor")
        paymentRepository.deleteAll()
    }
}
