package wtf.milehimikey.coffeeshop.reporting

import org.axonframework.messaging.queryhandling.annotation.QueryHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

// Queries
data class FindRevenueByDate(val date: LocalDate)
data class FindAllRevenue(val limit: Int = 30)

// Views
data class RevenueView(
    val date: LocalDate,
    val currency: String,
    val orderCount: Int,
    val orderRevenue: BigDecimal,
    val paymentCount: Int,
    val paymentAmount: BigDecimal
)

// Query Handlers
@Component
class ReportingQueryHandler(private val dailyRevenueRepository: DailyRevenueRepository) {

    @QueryHandler
    @Transactional(readOnly = true)
    fun handle(query: FindRevenueByDate): RevenueView? {
        return dailyRevenueRepository.findById(query.date)
            .map { it.toView() }
            .orElse(null)
    }

    @QueryHandler
    @Transactional(readOnly = true)
    fun handle(query: FindAllRevenue): List<RevenueView> {
        return dailyRevenueRepository.findAllByOrderBySummaryDateDesc()
            .take(query.limit)
            .map { it.toView() }
    }

    private fun DailyRevenue.toView(): RevenueView {
        return RevenueView(
            date = this.summaryDate,
            currency = this.currency,
            orderCount = this.orderCount,
            orderRevenue = this.orderRevenue,
            paymentCount = this.paymentCount,
            paymentAmount = this.paymentAmount
        )
    }
}
