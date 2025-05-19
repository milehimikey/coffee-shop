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
 */
@Document(collection = "idempotency_records")
@CompoundIndexes(
    CompoundIndex(name = "event_entity_processor_idx", def = "{\"eventId\": 1, \"entityId\": 1, \"processingGroup\": 1}", unique = true)
)
data class IdempotencyRecord(
    @Id val id: String,
    val eventId: String,
    val entityId: String,
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
     * Find a record by event ID, entity ID, and processing group.
     */
    fun findByEventIdAndEntityIdAndProcessingGroup(eventId: String, entityId: String, processingGroup: String): IdempotencyRecord?
    
    /**
     * Find all records for a specific entity ID and processing group.
     */
    fun findByEntityIdAndProcessingGroup(entityId: String, processingGroup: String): List<IdempotencyRecord>
    
    /**
     * Find all records for a specific processing group.
     */
    fun findByProcessingGroup(processingGroup: String): List<IdempotencyRecord>
}
