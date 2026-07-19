package wtf.milehimikey.coffeeshop.payments

import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@EventSourced(tagKey = "paymentId")
class Payment {

    private lateinit var id: String
    private lateinit var orderId: String
    private lateinit var amount: BigDecimal
    private var status: PaymentStatus = PaymentStatus.PENDING

    companion object {
        @JvmStatic
        @CommandHandler
        fun create(command: CreatePayment, appender: EventAppender): String {
            appender.append(PaymentCreated(id = command.id, orderId = command.orderId, amount = command.amount))
            return command.id
        }
    }

    @EntityCreator
    constructor(event: PaymentCreated) {
        id = event.id
        orderId = event.orderId
        amount = event.amount
        status = PaymentStatus.PENDING
    }

    @CommandHandler
    fun handle(command: ProcessPayment, appender: EventAppender) {
        if (status != PaymentStatus.PENDING) {
            throw IllegalStateException("Cannot process a payment that is not in PENDING status")
        }
        appender.append(
            PaymentProcessed(
                paymentId = id,
                orderId = orderId,
                amount = amount,
                transactionId = UUID.randomUUID().toString(),
                processedAt = Instant.now()
            )
        )
    }

    @CommandHandler
    fun handle(command: FailPayment, appender: EventAppender) {
        if (status != PaymentStatus.PENDING) {
            throw IllegalStateException("Cannot fail a payment that is not in PENDING status")
        }
        appender.append(
            PaymentFailed(
                paymentId = id,
                orderId = orderId,
                amount = amount,
                reason = command.reason,
                failedAt = Instant.now()
            )
        )
    }

    @CommandHandler
    fun handle(command: RefundPayment, appender: EventAppender) {
        if (status != PaymentStatus.PROCESSED) {
            throw IllegalStateException("Cannot refund a payment that is not in PROCESSED status")
        }
        appender.append(
            PaymentRefunded(
                paymentId = id,
                orderId = orderId,
                amount = amount,
                refundId = UUID.randomUUID().toString(),
                refundedAt = Instant.now()
            )
        )
    }

    @CommandHandler
    fun handle(command: ResetPayment, appender: EventAppender) {
        appender.append(
            PaymentReset(
                paymentId = id,
                orderId = orderId,
                amount = amount,
                resetAt = Instant.now()
            )
        )
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

    @EventSourcingHandler
    fun on(event: PaymentReset) {
        status = PaymentStatus.PENDING
    }
}

enum class PaymentStatus {
    PENDING, PROCESSED, FAILED, REFUNDED
}
