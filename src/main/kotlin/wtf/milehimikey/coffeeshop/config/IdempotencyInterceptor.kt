package wtf.milehimikey.coffeeshop.config

import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventMessage
import org.axonframework.messaging.InterceptorChain
import org.axonframework.messaging.MessageHandlerInterceptor
import org.axonframework.messaging.unitofwork.UnitOfWork
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

/**
 * Interceptor that ensures idempotent processing of events by tracking processed events
 * using their entity ID and event ID.
 *
 * This interceptor:
 * 1. Extracts the entityId from event headers or payload
 * 2. Checks if the event has already been processed
 * 3. Stores a record of processed events
 * 4. Is replay-aware (handles event replays appropriately)
 */
@Component
class IdempotencyInterceptor(private val idempotencyRepository: IdempotencyRepository) : MessageHandlerInterceptor<EventMessage<*>> {

    private val logger = LoggerFactory.getLogger(IdempotencyInterceptor::class.java)

    companion object {
        // Header keys
        const val ENTITY_ID_HEADER = "entityId"
        const val AGGREGATE_ID_HEADER = "aggregateId"
        const val AXON_AGGREGATE_ID_HEADER = "axonAggregateId"
        const val AXON_AGGREGATE_TYPE_HEADER = "axonAggregateType"
        const val AXON_AGGREGATE_SEQUENCE_HEADER = "axonAggregateSequence"
        const val REPLAY_FLAG_HEADER = "axonReplay"
    }

    override fun handle(unitOfWork: UnitOfWork<out EventMessage<*>>, interceptorChain: InterceptorChain): Any? {
        val eventMessage = unitOfWork.message as EventMessage<*>
        val processingGroup = getProcessingGroup(unitOfWork)
        val eventId = eventMessage.identifier
        val entityId = extractEntityId(eventMessage)
        val isReplay = isReplayEvent(eventMessage)

        // If we can't determine the entity ID, just proceed with processing
        if (entityId == null) {
            logger.debug("No entity ID found for event {}, proceeding with processing", eventId)
            return interceptorChain.proceed()
        }

        // Check if this event has already been processed
        val existingRecord = idempotencyRepository.findByEventIdAndEntityIdAndProcessingGroup(
            eventId, entityId, processingGroup
        )

        // If this is a replay event, we handle it differently
        if (isReplay) {
            // During replay, we want to process the event again even if it was processed before
            // But we'll update the record to mark it as a replay
            val result = interceptorChain.proceed()

            // Update or create the idempotency record
            if (existingRecord != null) {
                // Update existing record to mark it as a replay
                val updatedRecord = existingRecord.copy(isReplay = true)
                idempotencyRepository.save(updatedRecord)
                logger.debug("Updated existing idempotency record for replayed event: {}", eventId)
            } else {
                // Create a new record for this replayed event
                saveIdempotencyRecord(eventId, entityId, processingGroup, eventMessage, true)
                logger.debug("Created new idempotency record for replayed event: {}", eventId)
            }

            return result
        } else {
            // Normal processing (not a replay)
            if (existingRecord != null) {
                // Event has already been processed, skip it
                logger.debug("Event {} for entity {} has already been processed, skipping", eventId, entityId)
                return null
            }

            // Process the event
            val result = interceptorChain.proceed()

            // Save the idempotency record
            saveIdempotencyRecord(eventId, entityId, processingGroup, eventMessage, false)
            logger.debug("Created idempotency record for event: {}", eventId)

            return result
        }
    }

    /**
     * Extract the entity ID from the event message.
     * Tries several common header locations, then falls back to payload inspection.
     */
    private fun extractEntityId(eventMessage: EventMessage<*>): String? {
        val metadata = eventMessage.metaData

        // Try to get from common header locations
        return when {
            metadata.containsKey(ENTITY_ID_HEADER) -> metadata[ENTITY_ID_HEADER]?.toString()
            metadata.containsKey(AGGREGATE_ID_HEADER) -> metadata[AGGREGATE_ID_HEADER]?.toString()
            metadata.containsKey(AXON_AGGREGATE_ID_HEADER) -> metadata[AXON_AGGREGATE_ID_HEADER]?.toString()
            else -> extractEntityIdFromPayload(eventMessage.payload)
        }
    }

    /**
     * Attempt to extract entity ID from the payload.
     * Looks for common ID field names in the payload.
     */
    private fun extractEntityIdFromPayload(payload: Any): String? {
        // Try to find an ID field in the payload using reflection
        return try {
            val idField = payload.javaClass.declaredFields.firstOrNull {
                it.name == "id" || it.name == "entityId" || it.name == "aggregateId" || it.name.endsWith("Id")
            }

            if (idField != null) {
                idField.isAccessible = true
                idField.get(payload)?.toString()
            } else {
                // If we can't find an ID field, use the payload's hash code as a last resort
                payload.hashCode().toString()
            }
        } catch (e: Exception) {
            logger.warn("Failed to extract entity ID from payload: {}", e.message)
            null
        }
    }

    /**
     * Determine if this is a replay event.
     */
    private fun isReplayEvent(eventMessage: EventMessage<*>): Boolean {
        val metadata = eventMessage.metaData
        return metadata.containsKey(REPLAY_FLAG_HEADER) && metadata[REPLAY_FLAG_HEADER] == true
    }

    /**
     * Get the processing group for the current unit of work.
     */
    private fun getProcessingGroup(unitOfWork: UnitOfWork<out EventMessage<*>>): String {
        // Try to get from the message handler's class
        try {
            // Access the handler through reflection since there's no direct API for this
            val handlerField = unitOfWork.javaClass.getDeclaredField("handler")
            handlerField.isAccessible = true
            val handler = handlerField.get(unitOfWork)

            if (handler != null) {
                val handlerClass = handler.javaClass
                val processingGroupAnnotation = handlerClass.getAnnotation(ProcessingGroup::class.java)

                if (processingGroupAnnotation != null) {
                    return processingGroupAnnotation.value
                }

                // Fall back to the package name as per Axon's default behavior
                return handlerClass.`package`.name
            }
        } catch (e: Exception) {
            logger.debug("Could not determine processing group from handler: {}", e.message)
        }

        // If we can't determine the processing group, use a default
        return "default"
    }

    /**
     * Save an idempotency record for the processed event.
     */
    private fun saveIdempotencyRecord(
        eventId: String,
        entityId: String,
        processingGroup: String,
        eventMessage: EventMessage<*>,
        isReplay: Boolean
    ) {
        val record = IdempotencyRecord(
            id = UUID.randomUUID().toString(),
            eventId = eventId,
            entityId = entityId,
            processingGroup = processingGroup,
            headers = eventMessage.metaData,
            isReplay = isReplay
        )

        idempotencyRepository.save(record)
    }
}
