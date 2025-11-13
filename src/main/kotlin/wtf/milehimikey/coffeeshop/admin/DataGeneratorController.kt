package wtf.milehimikey.coffeeshop.admin

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import wtf.milehimikey.coffeeshop.config.DeadLetterProcessor
import java.util.*

/**
 * Controller for the data generation UI.
 * Provides a web interface for generating data and triggering dead letters.
 */
@Controller
@RequestMapping("/generator")
class DataGeneratorController(
    private val dataGenerator: DataGenerator,
    private val deadLetterProcessor: Optional<DeadLetterProcessor>
) {

    /**
     * Display the main data generator form.
     */
    @GetMapping
    fun showGeneratorForm(model: Model): String {
        return "generator"
    }

    /**
     * Handle batch data generation.
     */
    @PostMapping("/batch")
    fun generateBatch(
        @RequestParam productCount: Int,
        @RequestParam orderCount: Int,
        @RequestParam(defaultValue = "true") triggerSnapshots: Boolean,
        @RequestParam(defaultValue = "true") triggerDeadLetters: Boolean,
        redirectAttributes: RedirectAttributes
    ): String {
        val result = dataGenerator.generateBatch(
            productCount = productCount,
            orderCount = orderCount,
            triggerSnapshots = triggerSnapshots,
            triggerDeadLetters = triggerDeadLetters
        )

        redirectAttributes.addFlashAttribute("message",
            "Successfully generated ${result.productCount} products, " +
            "${result.orderCount} orders, and ${result.paymentCount} payments. " +
            "Snapshots triggered: ${result.snapshotsTriggered}, " +
            "Dead letters triggered: ${result.deadLettersTriggered}"
        )

        return "redirect:/generator"
    }

    /**
     * Handle product generation.
     */
    @PostMapping("/products")
    fun generateProducts(
        @RequestParam count: Int,
        @RequestParam(defaultValue = "false") triggerSnapshot: Boolean,
        redirectAttributes: RedirectAttributes
    ): String {
        val productIds = dataGenerator.generateProducts(
            count = count,
            triggerSnapshot = triggerSnapshot
        )

        redirectAttributes.addFlashAttribute("message",
            "Successfully generated ${productIds.size} products. " +
            "Snapshot triggered: $triggerSnapshot"
        )

        return "redirect:/generator"
    }

    /**
     * Handle order generation.
     */
    @PostMapping("/orders")
    fun generateOrders(
        @RequestParam count: Int,
        @RequestParam(required = false) customerId: String?,
        @RequestParam(required = false) itemsPerOrder: Int?,
        @RequestParam(defaultValue = "false") triggerSnapshot: Boolean,
        @RequestParam(defaultValue = "true") completeOrders: Boolean,
        redirectAttributes: RedirectAttributes
    ): String {
        val orderIds = dataGenerator.generateOrders(
            count = count,
            customerId = customerId,
            itemsPerOrder = itemsPerOrder,
            triggerSnapshot = triggerSnapshot,
            completeOrders = completeOrders
        )

        redirectAttributes.addFlashAttribute("message",
            "Successfully generated ${orderIds.size} orders. " +
            "Snapshot triggered: $triggerSnapshot, " +
            "Orders completed: $completeOrders"
        )

        return "redirect:/generator"
    }

    /**
     * Handle dead letter triggering.
     */
    @PostMapping("/deadletters/trigger")
    fun triggerDeadLetters(
        @RequestParam(defaultValue = "all") processor: String,
        redirectAttributes: RedirectAttributes
    ): String {
        val result = when (processor) {
            "payment" -> {
                val message = dataGenerator.triggerPaymentDeadLetter()
                mapOf("payment" to message)
            }
            "product" -> {
                val message = dataGenerator.triggerProductDeadLetter()
                mapOf("product" to message)
            }
            "order" -> {
                val message = dataGenerator.triggerOrderDeadLetter()
                mapOf("order" to message)
            }
            else -> dataGenerator.triggerDeadLetters().results
        }

        redirectAttributes.addFlashAttribute("message",
            "Dead letter triggering results: $result"
        )

        return "redirect:/generator"
    }

    /**
     * Handle dead letter processing.
     */
    @PostMapping("/deadletters/process")
    fun processDeadLetters(
        @RequestParam processingGroup: String,
        @RequestParam count: Int,
        redirectAttributes: RedirectAttributes
    ): String {
        deadLetterProcessor.ifPresent { dlProcessor ->
            val result = dlProcessor.processDeadLettersManually(
            processingGroup = processingGroup,
            count = count)

            redirectAttributes.addFlashAttribute("message",
                "Dead letter processing results for group '$processingGroup': " +
                        "Processed: ${result["processed"]}, " +
                        "Failed: ${result["failed"]}, " +
                        "Ignored: ${result["ignored"]}"
            )
        }

        return "redirect:/generator"
    }

    /**
     * Handle legacy product generation.
     * Generates products WITHOUT SKU fields to demonstrate the upcaster.
     */
    @PostMapping("/legacy-products")
    fun generateLegacyProducts(
        @RequestParam(defaultValue = "5") count: Int,
        redirectAttributes: RedirectAttributes
    ): String {
        val productIds = dataGenerator.generateLegacyProducts(count)

        redirectAttributes.addFlashAttribute("message",
            "Successfully generated ${productIds.size} legacy products (without SKU). " +
            "These products will have SKU added by the ProductCreatedUpcaster when events are replayed. " +
            "Click 'Demonstrate Upcaster' to see it in action!"
        )

        return "redirect:/generator"
    }

    /**
     * Handle upcaster demonstration.
     * Demonstrates the upcaster by querying and updating a legacy product.
     */
    @PostMapping("/demonstrate-upcaster")
    fun demonstrateUpcaster(
        @RequestParam(required = false) productId: String?,
        redirectAttributes: RedirectAttributes
    ): String {
        val result = dataGenerator.demonstrateUpcaster(productId)

        redirectAttributes.addFlashAttribute("message",
            if (result.success) {
                "Upcaster demonstration successful! ${result.message} " +
                "Check the application logs for detailed upcaster activity."
            } else {
                "Upcaster demonstration failed: ${result.message}"
            }
        )

        return "redirect:/generator"
    }
}
