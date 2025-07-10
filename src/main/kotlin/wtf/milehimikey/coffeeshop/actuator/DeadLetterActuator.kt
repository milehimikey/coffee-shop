package wtf.milehimikey.coffeeshop.actuator

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import wtf.milehimikey.coffeeshop.DataGenerator
import wtf.milehimikey.coffeeshop.config.DeadLetterProcessor

/**
 * Spring Boot Actuator-style endpoint for managing dead letters.
 * Provides operations to process and trigger dead letters through actuator-style endpoints.
 */
@RestController
@RequestMapping("/actuator/deadletters")
class DeadLetterActuator(
    private val dataGenerator: DataGenerator,
    @Autowired(required = false) private val deadLetterProcessor: DeadLetterProcessor?
) {

    /**
     * Get information about dead letter processing capabilities.
     */
    @GetMapping
    fun info(): ResponseEntity<Map<String, Any>> {
        val info = mapOf(
            "processorAvailable" to (deadLetterProcessor != null),
            "supportedProcessingGroups" to listOf("payment", "order", "product"),
            "operations" to mapOf(
                "process" to "POST /actuator/deadletters/process/{processingGroup}?count={count}",
                "trigger" to "POST /actuator/deadletters/trigger/{processor}",
                "triggerAll" to "POST /actuator/deadletters/trigger"
            )
        )
        return ResponseEntity.ok(info)
    }

    /**
     * Process dead letters for a specific processing group.
     */
    @PostMapping("/process/{processingGroup}")
    fun processDeadLetters(
        @PathVariable processingGroup: String,
        @RequestParam(defaultValue = "10") count: Int
    ): ResponseEntity<Map<String, Any>> {
        if (deadLetterProcessor == null) {
            val error = mapOf(
                "error" to "DeadLetterProcessor is not available",
                "processingGroup" to processingGroup,
                "count" to count
            )
            return ResponseEntity.ok(error)
        }

        val result = deadLetterProcessor.processDeadLettersManually(processingGroup, count)
        val response = mapOf(
            "processingGroup" to processingGroup,
            "requestedCount" to count,
            "results" to result
        )
        return ResponseEntity.ok(response)
    }

    /**
     * Trigger dead letters for a specific processor.
     */
    @PostMapping("/trigger/{processor}")
    fun triggerDeadLetters(@PathVariable processor: String): ResponseEntity<Map<String, Any>> {
        val response = when (processor.lowercase()) {
            "payment" -> {
                val result = dataGenerator.triggerPaymentDeadLetter()
                mapOf(
                    "processor" to "payment",
                    "result" to result
                )
            }
            "product" -> {
                val result = dataGenerator.triggerProductDeadLetter()
                mapOf(
                    "processor" to "product",
                    "result" to result
                )
            }
            "order" -> {
                val result = dataGenerator.triggerOrderDeadLetter()
                mapOf(
                    "processor" to "order",
                    "result" to result
                )
            }
            "all" -> {
                val result = dataGenerator.triggerDeadLetters()
                mapOf(
                    "processor" to "all",
                    "results" to result.results
                )
            }
            else -> {
                mapOf(
                    "error" to "Unknown processor: $processor",
                    "supportedProcessors" to listOf("payment", "product", "order", "all")
                )
            }
        }
        return ResponseEntity.ok(response)
    }

    /**
     * Trigger dead letters for all processors.
     */
    @PostMapping("/trigger")
    fun triggerAllDeadLetters(): ResponseEntity<Map<String, Any>> {
        val result = dataGenerator.triggerDeadLetters()
        val response = mapOf(
            "processor" to "all",
            "results" to result.results
        )
        return ResponseEntity.ok(response)
    }
}
