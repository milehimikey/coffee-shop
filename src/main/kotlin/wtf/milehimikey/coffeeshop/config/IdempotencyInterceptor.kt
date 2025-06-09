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
 * using their unique event ID.
 *
 * This interceptor:
 * 1. Uses the eventId (from eventMessage.identifier) as the primary idempotency key
 * 2. Extracts aggregateId from Axon headers for additional context
 * 3. Maps all Axon event headers appropriately
 * 4. Checks if the event has already been processed
 * 5. Stores a record of processed events
 * 6. Is replay-aware (handles event replays appropriately)
 */
@Component
class IdempotencyInterceptor(private val idempotencyRepository: IdempotencyRepository) : MessageHandlerInterceptor<EventMessage<*>> {

    private val logger = LoggerFactory.getLogger(IdempotencyInterceptor::class.java)

    companion object {
        // Axon Framework header keys (based on actual Axon implementation)
        const val AXON_MESSAGE_ID = "axon-message-id"
        const val AXON_MESSAGE_AGGREGATE_ID = "axon-message-aggregate-id"
        const val AXON_MESSAGE_AGGREGATE_TYPE = "axon-message-aggregate-type"
        const val AXON_MESSAGE_AGGREGATE_SEQ = "axon-message-aggregate-seq"
        const val AXON_MESSAGE_TIMESTAMP = "axon-message-timestamp"
        const val AXON_REPLAY = "axon-replay"

        // Legacy header keys for backward compatibility
        const val ENTITY_ID_HEADER = "entityId"
        const val AGGREGATE_ID_HEADER = "aggregateId"
    }

    override fun handle(unitOfWork: UnitOfWork<out EventMessage<*>>, interceptorChain: InterceptorChain): Any? {
        val eventMessage = unitOfWork.message as EventMessage<*>
        val processingGroup = getProcessingGroup(unitOfWork)
        val eventId = eventMessage.identifier
        val aggregateId = extractAggregateId(eventMessage)
        val isReplay = isReplayEvent(eventMessage)
        val allHeaders = extractAllAxonHeaders(eventMessage)

        logger.debug("Processing event {} with aggregateId {} in processing group {}, isReplay: {}",
                    eventId, aggregateId, processingGroup, isReplay)

        // Check if this event has already been processed (using eventId as primary key)
        val existingRecord = idempotencyRepository.findByEventIdAndProcessingGroup(eventId, processingGroup)

        // If this is a replay event, we handle it differently
        if (isReplay) {
            // During replay, we want to process the event again even if it was processed before
            // But we'll update the record to mark it as a replay
            val result = interceptorChain.proceed()

            // Update or create the idempotency record
            if (existingRecord != null) {
                // Update existing record to mark it as a replay
                val updatedRecord = existingRecord.copy(
                    isReplay = true,
                    headers = allHeaders,
                    aggregateId = aggregateId ?: existingRecord.aggregateId
                )
                idempotencyRepository.save(updatedRecord)
                logger.debug("Updated existing idempotency record for replayed event: {}", eventId)
            } else {
                // Create a new record for this replayed event
                saveIdempotencyRecord(eventId, aggregateId, processingGroup, allHeaders, true)
                logger.debug("Created new idempotency record for replayed event: {}", eventId)
            }

            return result
        } else {
            // Normal processing (not a replay)
            if (existingRecord != null) {
                // Event has already been processed, skip it
                logger.debug("Event {} has already been processed in processing group {}, skipping", eventId, processingGroup)
                return null
            }

            // Process the event
            val result = interceptorChain.proceed()

            // Save the idempotency record
            saveIdempotencyRecord(eventId, aggregateId, processingGroup, allHeaders, false)
            logger.debug("Created idempotency record for event: {} with aggregateId: {}", eventId, aggregateId)

            return result
        }
    }

    /**
     * Extract the aggregate ID from the event message.
     * Tries Axon headers first, then falls back to legacy headers and payload inspection.
     */
    private fun extractAggregateId(eventMessage: EventMessage<*>): String? {
        val metadata = eventMessage.metaData

        // Try to get from Axon headers first
        return when {
            metadata.containsKey(AXON_MESSAGE_AGGREGATE_ID) -> metadata[AXON_MESSAGE_AGGREGATE_ID]?.toString()
            metadata.containsKey(AGGREGATE_ID_HEADER) -> metadata[AGGREGATE_ID_HEADER]?.toString()
            metadata.containsKey(ENTITY_ID_HEADER) -> metadata[ENTITY_ID_HEADER]?.toString()
            else -> extractAggregateIdFromPayload(eventMessage.payload)
        }
    }

    /**
     * Extract all Axon headers from the event message for comprehensive tracking.
     */
    private fun extractAllAxonHeaders(eventMessage: EventMessage<*>): Map<String, Any?> {
        val metadata = eventMessage.metaData
        val axonHeaders = mutableMapOf<String, Any?>()

        // Extract all known Axon headers
        listOf(
            AXON_MESSAGE_ID,
            AXON_MESSAGE_AGGREGATE_ID,
            AXON_MESSAGE_AGGREGATE_TYPE,
            AXON_MESSAGE_AGGREGATE_SEQ,
            AXON_MESSAGE_TIMESTAMP,
            AXON_REPLAY
        ).forEach { headerKey ->
            if (metadata.containsKey(headerKey)) {
                axonHeaders[headerKey] = metadata[headerKey]
            }
        }

        // Also include legacy headers if present
        listOf(ENTITY_ID_HEADER, AGGREGATE_ID_HEADER).forEach { headerKey ->
            if (metadata.containsKey(headerKey)) {
                axonHeaders[headerKey] = metadata[headerKey]
            }
        }

        // Include all metadata for comprehensive tracking
        axonHeaders.putAll(metadata)

        return axonHeaders
    }

    /**
     * Attempt to extract aggregate ID from the payload.
     * Looks for common ID field names in the payload.
     */
    private fun extractAggregateIdFromPayload(payload: Any): String? {
        // Try to find an ID field in the payload using reflection
        return try {
            val idField = payload.javaClass.declaredFields.firstOrNull {
                it.name == "id" || it.name == "entityId" || it.name == "aggregateId" || it.name.endsWith("Id")
            }

            if (idField != null) {
                idField.isAccessible = true
                idField.get(payload)?.toString()
            } else {
                logger.debug("No ID field found in payload of type: {}", payload.javaClass.simpleName)
                null
            }
        } catch (e: Exception) {
            logger.warn("Failed to extract aggregate ID from payload: {}", e.message)
            null
        }
    }

    /**
     * Determine if this is a replay event.
     * Checks both the official Axon replay header and legacy header.
     */
    private fun isReplayEvent(eventMessage: EventMessage<*>): Boolean {
        val metadata = eventMessage.metaData
        return (metadata.containsKey(AXON_REPLAY) && metadata[AXON_REPLAY] == true) ||
               (metadata.containsKey("axonReplay") && metadata["axonReplay"] == true)
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
        aggregateId: String?,
        processingGroup: String,
        headers: Map<String, Any?>,
        isReplay: Boolean
    ) {
        val record = IdempotencyRecord(
            id = UUID.randomUUID().toString(),
            eventId = eventId,
            aggregateId = aggregateId ?: "unknown",
            processingGroup = processingGroup,
            headers = headers,
            isReplay = isReplay
        )

        idempotencyRepository.save(record)
    }
}
