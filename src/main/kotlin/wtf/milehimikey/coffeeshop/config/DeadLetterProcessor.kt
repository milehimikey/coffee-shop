package wtf.milehimikey.coffeeshop.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.axonframework.messaging.eventhandling.processing.errorhandling.ErrorContext
import org.axonframework.messaging.eventhandling.processing.errorhandling.ErrorHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class DeadLetterProcessor(
    private val failedEventRepository: FailedEventRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(DeadLetterProcessor::class.java)

    fun errorHandler(): ErrorHandler = ErrorHandler { context: ErrorContext ->
        persistFailedEvents(context)
    }

    private fun persistFailedEvents(context: ErrorContext) {
        context.failedEvents().forEach { event ->
            val eventPayload: Any = event.payload() ?: "unknown"
            val payload = try {
                objectMapper.writeValueAsString(eventPayload)
            } catch (e: Exception) {
                eventPayload.toString()
            }
            val aggregateId = event.metadata()["aggregateId"]?.toString()
                ?: event.metadata()["axon-message-aggregate-id"]?.toString()
                ?: "unknown"
            val record = FailedEventRecord(
                processingGroup = context.eventProcessor(),
                eventType = eventPayload.javaClass.simpleName,
                aggregateId = aggregateId,
                sequenceIdentifier = event.identifier(),
                payload = payload,
                errorMessage = context.error().message ?: context.error().javaClass.simpleName
            )
            try {
                failedEventRepository.save(record)
                logger.warn(
                    "Stored failed event {} ({}) in group '{}': {}",
                    event.identifier(), record.eventType, context.eventProcessor(), record.errorMessage
                )
            } catch (e: Exception) {
                logger.error("Failed to persist dead letter record for event {}", event.identifier(), e)
            }
        }
    }

    fun processDeadLettersManually(processingGroup: String, count: Int): Map<String, Int> {
        val pending = failedEventRepository.findByProcessingGroup(processingGroup)
            .take(count)

        if (pending.isEmpty()) {
            return mapOf("processed" to 0, "failed" to 0, "ignored" to 0)
        }

        var processed = 0
        var failed = 0

        pending.forEach { record ->
            try {
                record.retryCount++
                record.lastRetryAt = java.time.Instant.now()
                failedEventRepository.save(record)
                processed++
                logger.info("Marked dead letter {} as retried (attempt #{})", record.id, record.retryCount)
            } catch (e: Exception) {
                failed++
                logger.error("Failed to update dead letter record {}", record.id, e)
            }
        }

        return mapOf("processed" to processed, "failed" to failed, "ignored" to 0)
    }

    fun getDeadLetterViews(processingGroup: String? = null): List<DeadLetterView> {
        val records = if (processingGroup != null) {
            failedEventRepository.findByProcessingGroup(processingGroup)
        } else {
            failedEventRepository.findAll()
        }
        return records.map { record ->
            DeadLetterView(
                sequenceIdentifier = record.sequenceIdentifier,
                processingGroup = record.processingGroup,
                causeMessage = record.errorMessage,
                lastTouched = record.lastRetryAt ?: record.failedAt,
                diagnostics = mapOf(
                    "eventType" to record.eventType,
                    "aggregateId" to record.aggregateId,
                    "retryCount" to record.retryCount.toString(),
                    "failedAt" to record.failedAt.toString()
                )
            )
        }
    }
}
