package wtf.milehimikey.coffeeshop.reporting

import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import wtf.milehimikey.coffeeshop.orders.OrderCompleted
import wtf.milehimikey.coffeeshop.payments.PaymentProcessed
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * JPA projection maintaining the [DailyRevenue] rollup on PostgreSQL.
 *
 * Handlers are assigned to their own `reporting` processor in `AxonConfig.kt`, selected by
 * package, so this read model has its own tracking token and can be replayed independently
 * of the Mongo projections.
 */
@Component
class ReportingProjection(private val dailyRevenueRepository: DailyRevenueRepository) {

    private val logger = LoggerFactory.getLogger(ReportingProjection::class.java)

    @EventHandler
    @Transactional
    fun on(event: OrderCompleted) {
        val date = event.completedAt.toUtcDate()
        val summary = summaryFor(date, event.totalAmount.currency.currencyCode)
        summary.orderCount += 1
        // moneta's numberStripped gives the BigDecimal without trailing zeros
        summary.orderRevenue = summary.orderRevenue.add(event.totalAmount.numberStripped)
        dailyRevenueRepository.save(summary)
        logger.info("Reporting: order {} added {} to {}", event.orderId, event.totalAmount, date)
    }

    @EventHandler
    @Transactional
    fun on(event: PaymentProcessed) {
        val date = event.processedAt.toUtcDate()
        val summary = summaryFor(date)
        summary.paymentCount += 1
        summary.paymentAmount = summary.paymentAmount.add(event.amount)
        dailyRevenueRepository.save(summary)
        logger.info("Reporting: payment {} added {} to {}", event.paymentId, event.amount, date)
    }

    private fun summaryFor(date: LocalDate, currency: String? = null): DailyRevenue {
        val summary = dailyRevenueRepository.findById(date)
            .orElseGet { DailyRevenue(summaryDate = date) }
        if (currency != null) {
            summary.currency = currency
        }
        return summary
    }

    private fun Instant.toUtcDate(): LocalDate = this.atZone(ZoneOffset.UTC).toLocalDate()
}
