package wtf.milehimikey.coffeeshop.config

import org.axonframework.messaging.core.MessageHandlerInterceptor
import org.axonframework.messaging.core.MessageHandlerInterceptorChain
import org.axonframework.messaging.core.MessageStream
import org.axonframework.messaging.core.unitofwork.ProcessingContext
import org.axonframework.messaging.eventhandling.EventMessage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

/**
 * Interceptor that ensures idempotent processing of events by tracking processed events
 * using their unique event ID. In Axon 5, this is registered automatically via Spring
 * autoconfiguration as a MessageHandlerInterceptor<EventMessage> bean.
 */
@Component
class IdempotencyInterceptor(private val idempotencyRepository: IdempotencyRepository) :
    MessageHandlerInterceptor<EventMessage> {

    private val logger = LoggerFactory.getLogger(IdempotencyInterceptor::class.java)

    companion object {
        const val AXON_MESSAGE_AGGREGATE_ID = "axon-message-aggregate-id"
        const val AXON_REPLAY = "axon-replay"
    }

    override fun interceptOnHandle(
        message: EventMessage,
        context: ProcessingContext,
        chain: MessageHandlerInterceptorChain<EventMessage>
    ): MessageStream<*> {
        val eventId = message.identifier()
        val processingGroup = deriveProcessingGroupFromThread()
        val aggregateId = extractAggregateId(message)
        val isReplay = isReplayEvent(message)
        val allHeaders = message.metadata().toMap()

        logger.debug(
            "Processing event {} with aggregateId {} in group {}, isReplay: {}",
            eventId, aggregateId, processingGroup, isReplay
        )

        val existingRecord = idempotencyRepository.findByEventIdAndProcessingGroup(eventId, processingGroup)

        if (isReplay) {
            val result = chain.proceed(message, context)
            if (existingRecord != null) {
                idempotencyRepository.save(existingRecord.copy(isReplay = true, headers = allHeaders))
                logger.debug("Updated idempotency record for replayed event: {}", eventId)
            } else {
                saveIdempotencyRecord(eventId, aggregateId, processingGroup, allHeaders, true)
                logger.debug("Created idempotency record for replayed event: {}", eventId)
            }
            return result
        } else {
            if (existingRecord != null) {
                logger.debug("Event {} already processed in group {}, skipping", eventId, processingGroup)
                return MessageStream.empty<EventMessage>()
            }
            val result = chain.proceed(message, context)
            saveIdempotencyRecord(eventId, aggregateId, processingGroup, allHeaders, false)
            logger.debug("Created idempotency record for event: {} aggregateId: {}", eventId, aggregateId)
            return result
        }
    }

    private fun deriveProcessingGroupFromThread(): String {
        val threadName = Thread.currentThread().name
        val match = Regex("Processor\\[([^]]+)]").find(threadName)
        return match?.groupValues?.get(1) ?: "default"
    }

    private fun extractAggregateId(message: EventMessage): String? {
        val metadata = message.metadata()
        return metadata[AXON_MESSAGE_AGGREGATE_ID]
            ?: metadata["aggregateId"]
            ?: extractAggregateIdFromPayload(message.payload())
    }

    private fun extractAggregateIdFromPayload(payload: Any?): String? {
        if (payload == null) return null
        return try {
            val idField = payload.javaClass.declaredFields.firstOrNull {
                it.name == "id" || it.name == "entityId" || it.name == "aggregateId" || it.name.endsWith("Id")
            }
            idField?.let {
                it.isAccessible = true
                it.get(payload)?.toString()
            }
        } catch (e: Exception) {
            logger.warn("Failed to extract aggregate ID from payload: {}", e.message)
            null
        }
    }

    private fun isReplayEvent(message: EventMessage): Boolean {
        val metadata = message.metadata()
        return metadata[AXON_REPLAY] == "true" || metadata["axonReplay"] == "true"
    }

    private fun saveIdempotencyRecord(
        eventId: String,
        aggregateId: String?,
        processingGroup: String,
        headers: Map<String, Any?>,
        isReplay: Boolean
    ) {
        idempotencyRepository.save(
            IdempotencyRecord(
                id = UUID.randomUUID().toString(),
                eventId = eventId,
                aggregateId = aggregateId ?: "unknown",
                processingGroup = processingGroup,
                headers = headers,
                isReplay = isReplay
            )
        )
    }
}
