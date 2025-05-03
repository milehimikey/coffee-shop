package wtf.milehimikey.coffeeshop.config

import org.axonframework.config.EventProcessingConfiguration
import org.axonframework.messaging.deadletter.SequencedDeadLetterProcessor
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Component responsible for periodically processing dead-lettered events.
 * This attempts to reprocess events that have previously failed and been stored in the dead letter queue.
 */
@Component
@EnableScheduling
class DeadLetterProcessor(private val eventProcessingConfiguration: EventProcessingConfiguration) {

    private val logger = LoggerFactory.getLogger(DeadLetterProcessor::class.java)

    /**
     * Scheduled task that attempts to process any dead-lettered events for the payment processing group.
     * Runs every minute with an initial delay of 1 minute after application startup.
     */
    @Scheduled(fixedDelayString = "60000", initialDelayString = "60000")
    fun processDeadLetters() {
        logger.info("Attempting to process dead-lettered events for payment processing group")

        eventProcessingConfiguration.sequencedDeadLetterProcessor("payment")
            .ifPresent { processor ->
                try {
                    val result = processor.processAny()
                    when (result.toString()) {
                        "PROCESSED" -> logger.info("Successfully processed a dead-lettered event")
                        "FAILED" -> logger.warn("Failed to process a dead-lettered event, it will be retried later")
                        else -> logger.info("No dead-lettered events to process at this time")
                    }
                } catch (e: Exception) {
                    logger.error("Error while processing dead-lettered events", e)
                }
            }
    }
}
