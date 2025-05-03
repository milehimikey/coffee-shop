package wtf.milehimikey.coffeeshop.payments

import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.ResetHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

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

    /**
     * Pre-check method to determine if a payment has the error-triggering amount.
     * This is called before the actual event handler to avoid transaction issues.
     */
    fun shouldFailPaymentReset(paymentId: String): Boolean {
        val payment = paymentRepository.findById(paymentId)
        if (payment.isPresent && payment.get().amount.compareTo(java.math.BigDecimal("13.13")) == 0) {
            logger.info("Payment ${paymentId} has error-triggering amount $13.13")
            return true
        }
        return false
    }

    /**
     * Handler for PaymentReset events that intentionally throws an exception
     * when the payment amount is exactly $13.13 to demonstrate dead letter queue functionality.
     */
    @EventHandler
    fun on(event: PaymentReset) {
        logger.info("Processing PaymentReset event for payment ${event.paymentId}")

        // Check if this payment should fail before entering transaction
        if (shouldFailPaymentReset(event.paymentId)) {
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
                    updatedAt = Instant.now()
                )
            )
        }
    }

    /**
     * For testing purposes only - allows direct verification of error handling
     * without going through the event store.
     */
    fun processPaymentResetDirectly(paymentId: String) {
        try {
            // First check if this payment should fail
            if (shouldFailPaymentReset(paymentId)) {
                logger.error("Simulated error processing PaymentReset event for payment $paymentId with amount $13.13")
                throw RuntimeException("Simulated error processing PaymentReset event for payment with amount $13.13")
            }

            on(PaymentReset(paymentId = paymentId))
            logger.info("Successfully processed PaymentReset event for payment $paymentId")
        } catch (e: Exception) {
            logger.error("Error processing PaymentReset event for payment $paymentId", e)
            throw e
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
