package wtf.milehimikey.coffeeshop.config

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * MongoDB document for storing processed event information to ensure idempotency.
 * This tracks events that have been processed by event handlers to prevent duplicate processing.
 * Uses eventId as the primary idempotency key, with aggregateId for additional context.
 */
@Document(collection = "idempotency_records")
@CompoundIndexes(
    CompoundIndex(name = "event_processor_idx", def = "{\"eventId\": 1, \"processingGroup\": 1}", unique = true),
    CompoundIndex(name = "aggregate_processor_idx", def = "{\"aggregateId\": 1, \"processingGroup\": 1}")
)
data class IdempotencyRecord(
    @Id val id: String,
    val eventId: String,
    val aggregateId: String,
    val processingGroup: String,
    val timestamp: Instant = Instant.now(),
    val headers: Map<String, Any?>,
    val isReplay: Boolean = false
)

/**
 * Repository for IdempotencyRecord documents.
 */
@Repository
interface IdempotencyRepository : MongoRepository<IdempotencyRecord, String> {
    /**
     * Find a record by event ID and processing group.
     * This is the primary method for idempotency checking since eventId is unique.
     */
    fun findByEventIdAndProcessingGroup(eventId: String, processingGroup: String): IdempotencyRecord?

    /**
     * Find a record by event ID, aggregate ID, and processing group.
     * Kept for backward compatibility and additional filtering.
     */
    fun findByEventIdAndAggregateIdAndProcessingGroup(eventId: String, aggregateId: String, processingGroup: String): IdempotencyRecord?

    /**
     * Find all records for a specific aggregate ID and processing group.
     */
    fun findByAggregateIdAndProcessingGroup(aggregateId: String, processingGroup: String): List<IdempotencyRecord>

    /**
     * Find all records for a specific processing group.
     */
    fun findByProcessingGroup(processingGroup: String): List<IdempotencyRecord>
}
