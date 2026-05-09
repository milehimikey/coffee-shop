package wtf.milehimikey.coffeeshop.actuator

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import wtf.milehimikey.coffeeshop.admin.DataGenerator
import wtf.milehimikey.coffeeshop.config.DeadLetterProcessor

/**
 * Spring Boot Actuator-style endpoint for managing dead letters.
 * Provides operations to process and trigger dead letters through actuator-style endpoints.
 */
@RestController
@RequestMapping("/actuator/deadletters")
class DeadLetterActuator(
    private val dataGenerator: DataGenerator,
    private val deadLetterProcessor: DeadLetterProcessor
) {

    @GetMapping
    fun info(): ResponseEntity<Map<String, Any>> {
        val groups = listOf("payment", "order", "product")
        val counts = groups.associateWith { group ->
            deadLetterProcessor.getDeadLetterViews(group).size
        }
        val info = mapOf(
            "deadLetterCounts" to counts,
            "operations" to mapOf(
                "list" to "GET /actuator/deadletters/{processingGroup}",
                "process" to "POST /actuator/deadletters/process/{processingGroup}?count={count}",
                "trigger" to "POST /actuator/deadletters/trigger/{processor}"
            )
        )
        return ResponseEntity.ok(info)
    }

    @GetMapping("/{processingGroup}")
    fun listDeadLetters(@PathVariable processingGroup: String): ResponseEntity<List<Any>> {
        val views = deadLetterProcessor.getDeadLetterViews(processingGroup)
        return ResponseEntity.ok(views)
    }

    @PostMapping("/process/{processingGroup}")
    fun processDeadLetters(
        @PathVariable processingGroup: String,
        @RequestParam(defaultValue = "10") count: Int
    ): ResponseEntity<Map<String, Any>> {
        val result = deadLetterProcessor.processDeadLettersManually(processingGroup, count)
        return ResponseEntity.ok(mapOf(
            "processingGroup" to processingGroup,
            "requestedCount" to count,
            "results" to result
        ))
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
