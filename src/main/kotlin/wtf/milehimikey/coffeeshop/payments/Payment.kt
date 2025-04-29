package wtf.milehimikey.coffeeshop.payments

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.spring.stereotype.Aggregate
import org.axonframework.serialization.Revision
import java.math.BigDecimal
import java.util.*

@Aggregate(snapshotTriggerDefinition = "paymentSnapshotTriggerDefinition")
@Revision("1")
class Payment {

    @AggregateIdentifier
    lateinit var id: String
    private lateinit var orderId: String
    private lateinit var amount: BigDecimal
    private var status: PaymentStatus = PaymentStatus.PENDING

    constructor() // Required by Axon

    @CommandHandler
    constructor(command: CreatePayment) {
        AggregateLifecycle.apply(
            PaymentCreated(
                id = command.id,
                orderId = command.orderId,
                amount = command.amount
            )
        )
    }

    @CommandHandler
    fun handle(command: ProcessPayment) {
        if (status != PaymentStatus.PENDING) {
            throw IllegalStateException("Cannot process a payment that is not in PENDING status")
        }

        // In a real application, we would integrate with a payment gateway here
        // For this example, we'll simulate a successful payment
        AggregateLifecycle.apply(
            PaymentProcessed(
                paymentId = id,
                transactionId = UUID.randomUUID().toString()
            )
        )
    }

    @CommandHandler
    fun handle(command: FailPayment) {
        if (status != PaymentStatus.PENDING) {
            throw IllegalStateException("Cannot fail a payment that is not in PENDING status")
        }

        AggregateLifecycle.apply(
            PaymentFailed(
                paymentId = id,
                reason = command.reason
            )
        )
    }

    @CommandHandler
    fun handle(command: RefundPayment) {
        if (status != PaymentStatus.PROCESSED) {
            throw IllegalStateException("Cannot refund a payment that is not in PROCESSED status")
        }

        AggregateLifecycle.apply(
            PaymentRefunded(
                paymentId = id,
                refundId = UUID.randomUUID().toString()
            )
        )
    }

    /**
     * Command handler for ResetPayment command.
     * This is a special command used for testing to reset the payment status to PENDING.
     */
    @CommandHandler
    fun handle(command: ResetPayment) {
        // Allow resetting from any state for testing purposes
        AggregateLifecycle.apply(
            PaymentReset(
                paymentId = id
            )
        )
    }

    /**
     * Event sourcing handler for PaymentReset event.
     */
    @EventSourcingHandler
    fun on(event: PaymentReset) {
        status = PaymentStatus.PENDING
    }

    @EventSourcingHandler
    fun on(event: PaymentCreated) {
        id = event.id
        orderId = event.orderId
        amount = event.amount
        status = PaymentStatus.PENDING
    }

    @EventSourcingHandler
    fun on(event: PaymentProcessed) {
        status = PaymentStatus.PROCESSED
    }

    @EventSourcingHandler
    fun on(event: PaymentFailed) {
        status = PaymentStatus.FAILED
    }

    @EventSourcingHandler
    fun on(event: PaymentRefunded) {
        status = PaymentStatus.REFUNDED
    }
}

enum class PaymentStatus {
    PENDING, PROCESSED, FAILED, REFUNDED
}
