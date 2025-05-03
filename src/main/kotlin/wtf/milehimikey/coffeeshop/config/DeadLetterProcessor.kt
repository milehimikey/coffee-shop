package wtf.milehimikey.coffeeshop.config

import org.axonframework.config.EventProcessingConfiguration
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

    // List of all processing groups with dead letter queues
    private val processingGroups = listOf("payment", "order", "product")

    /**
     * Scheduled task that attempts to process any dead-lettered events for all processing groups.
     * Runs every minute with an initial delay of 1 minute after application startup.
     */
    @Scheduled(fixedDelayString = "60000", initialDelayString = "60000")
    fun processDeadLetters() {
        logger.info("Attempting to process dead-lettered events for all processing groups")

        // Process dead letters for each processing group
        processingGroups.forEach { processingGroup ->
            processDeadLettersForGroup(processingGroup)
        }
    }

    /**
     * Process dead letters for a specific processing group.
     *
     * @param processingGroup The name of the processing group
     */
    private fun processDeadLettersForGroup(processingGroup: String) {
        logger.info("Processing dead letters for group: $processingGroup")

        eventProcessingConfiguration.sequencedDeadLetterProcessor(processingGroup)
            .ifPresent { processor ->
                try {
                    val result = processor.processAny()
                    logProcessingResult(processingGroup, result)
                } catch (e: Exception) {
                    logger.error("Error while processing dead-lettered events for group $processingGroup", e)
                }
            }
    }

    /**
     * Log the result of processing a dead letter.
     *
     * @param processingGroup The name of the processing group
     * @param result The processing result
     */
    private fun logProcessingResult(processingGroup: String, result: Any) {
        when (result.toString()) {
            "PROCESSED" -> logger.info("Successfully processed a dead-lettered event for group $processingGroup")
            "FAILED" -> logger.warn("Failed to process a dead-lettered event for group $processingGroup, it will be retried later")
            else -> logger.info("No dead-lettered events to process for group $processingGroup at this time")
        }
    }

    /**
     * Manually process a specific number of dead letters for a given processing group.
     * This can be called from a REST endpoint to trigger processing on demand.
     *
     * @param processingGroup The name of the processing group
     * @param count The maximum number of dead letters to process
     * @return A map of results with counts of processed, failed, and ignored items
     */
    fun processDeadLettersManually(processingGroup: String, count: Int): Map<String, Int> {
        logger.info("Manually processing up to $count dead letters for group $processingGroup")

        val results = mutableMapOf(
            "processed" to 0,
            "failed" to 0,
            "ignored" to 0
        )

        eventProcessingConfiguration.sequencedDeadLetterProcessor(processingGroup)
            .ifPresent { processor ->
                try {
                    for (i in 1..count) {
                        val result = processor.processAny()
                        when (result.toString()) {
                            "PROCESSED" -> results["processed"] = results["processed"]!! + 1
                            "FAILED" -> results["failed"] = results["failed"]!! + 1
                            else -> {
                                results["ignored"] = results["ignored"]!! + 1
                                break  // No more items to process
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Error while manually processing dead-lettered events for group $processingGroup", e)
                }
            }

        logger.info("Manual processing results for group $processingGroup: $results")
        return results
    }
}
