package wtf.milehimikey.coffeeshop.reporting

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Daily revenue rollup, kept on PostgreSQL rather than MongoDB. This is the second read-model
 * technology fed from the same event stream: Mongo serves the per-context operational views,
 * Postgres serves reporting.
 *
 * Amounts are stored as [BigDecimal] with the currency in its own column. Orders carry a
 * `javax.money.MonetaryAmount` and payments carry a bare [BigDecimal], so both are normalised
 * on the way in rather than letting that inconsistency reach the schema.
 */
@Entity
@Table(name = "daily_revenue")
class DailyRevenue(
    @Id
    @Column(name = "summary_date", nullable = false)
    var summaryDate: LocalDate,

    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "USD",

    @Column(name = "order_count", nullable = false)
    var orderCount: Int = 0,

    @Column(name = "order_revenue", nullable = false, precision = 19, scale = 4)
    var orderRevenue: BigDecimal = BigDecimal.ZERO,

    @Column(name = "payment_count", nullable = false)
    var paymentCount: Int = 0,

    @Column(name = "payment_amount", nullable = false, precision = 19, scale = 4)
    var paymentAmount: BigDecimal = BigDecimal.ZERO
)

@Repository
interface DailyRevenueRepository : JpaRepository<DailyRevenue, LocalDate> {
    fun findAllByOrderBySummaryDateDesc(): List<DailyRevenue>
}
