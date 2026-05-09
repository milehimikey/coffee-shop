package wtf.milehimikey.coffeeshop.config

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Placeholder for custom dead letter processing.
 * The Axon 4 built-in Sequenced DLQ moved to commercial AxonIQ Framework in Axon 5.
 * Future implementation will use a custom FailedEventRecord JPA entity for persistence.
 */
@Component
class DeadLetterProcessor {

    private val logger = LoggerFactory.getLogger(DeadLetterProcessor::class.java)

    private val processingGroups = listOf("payment", "order", "product")

    fun processDeadLetters() {
        logger.info("Dead letter processing not yet implemented for Axon 5 OSS")
    }

    fun processDeadLettersManually(processingGroup: String, count: Int): Map<String, Int> {
        logger.info("Manual dead letter processing not yet implemented for Axon 5 OSS (group: $processingGroup, count: $count)")
        return mapOf("processed" to 0, "failed" to 0, "ignored" to count)
    }
}
