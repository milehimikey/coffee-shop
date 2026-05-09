package wtf.milehimikey.coffeeshop.config

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Entity
@Table(name = "failed_events")
data class FailedEventRecord(
    @Id val id: String = UUID.randomUUID().toString(),
    val processingGroup: String,
    val eventType: String,
    val aggregateId: String,
    val sequenceIdentifier: String,
    @Column(columnDefinition = "TEXT") val payload: String,
    @Column(columnDefinition = "TEXT") val errorMessage: String,
    val failedAt: Instant = Instant.now(),
    var retryCount: Int = 0,
    var lastRetryAt: Instant? = null
)

@Repository
interface FailedEventRepository : JpaRepository<FailedEventRecord, String> {
    fun findByProcessingGroup(processingGroup: String): List<FailedEventRecord>
    fun countByProcessingGroup(processingGroup: String): Long
}
