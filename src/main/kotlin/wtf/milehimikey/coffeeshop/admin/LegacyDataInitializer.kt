package wtf.milehimikey.coffeeshop.admin

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Component responsible for initializing legacy product data when the application starts.
 * This component is activated by the "legacy-data" Spring profile.
 * 
 * Purpose:
 * - Generates products WITHOUT SKU fields to demonstrate the ProductCreatedUpcaster
 * - These products simulate data that existed before the SKU field was added
 * - The upcaster will add SKU fields when these events are replayed from the event store
 * 
 * Usage:
 * Run the application with: --spring.profiles.active=legacy-data
 * 
 * This component is excluded from the test profile to avoid creating sample data during tests.
 */
@Component
@Profile("legacy-data & !test")
class LegacyDataInitializer(private val dataGenerator: DataGenerator) {
    private val logger = LoggerFactory.getLogger(LegacyDataInitializer::class.java)

    @EventListener(ApplicationStartedEvent::class)
    fun initialize() {
        logger.info("========================================")
        logger.info("LEGACY DATA INITIALIZATION")
        logger.info("========================================")
        logger.info("The 'legacy-data' profile is active.")
        logger.info("Generating legacy products without SKU fields...")
        logger.info("")

        try {
            // Generate 10 legacy products
            val productIds = dataGenerator.generateLegacyProducts(10)

            logger.info("")
            logger.info("========================================")
            logger.info("LEGACY DATA INITIALIZATION COMPLETE")
            logger.info("========================================")
            logger.info("Generated ${productIds.size} legacy products without SKU fields")
            logger.info("")
            logger.info("These products have ProductCreated events WITHOUT the SKU field.")
            logger.info("The ProductCreatedUpcaster will add SKU fields when events are replayed.")
            logger.info("")
            logger.info("HOW TO DEMONSTRATE THE UPCASTER:")
            logger.info("----------------------------------")
            logger.info("Option 1: Use the REST API")
            logger.info("  POST http://localhost:8080/api/generate/demonstrate-upcaster")
            logger.info("")
            logger.info("Option 2: Use the Admin UI")
            logger.info("  Navigate to: http://localhost:8080/generator")
            logger.info("  Click the 'Demonstrate Upcaster' button")
            logger.info("")
            logger.info("Option 3: Query a product and update it")
            logger.info("  1. GET http://localhost:8080/api/products")
            logger.info("  2. Find a product with '(Legacy' in the name")
            logger.info("  3. Note that it HAS a SKU (added by upcaster!)")
            logger.info("  4. PUT http://localhost:8080/api/products/{id} to update it")
            logger.info("  5. Watch the logs for upcaster activity")
            logger.info("")
            logger.info("WHAT TO LOOK FOR IN THE LOGS:")
            logger.info("------------------------------")
            logger.info("When you update a legacy product, you should see:")
            logger.info("  'Upcasting ProductCreated event for product <id>: adding SKU = <sku>'")
            logger.info("")
            logger.info("This proves the upcaster is working - it's adding the SKU field")
            logger.info("to old ProductCreated events that didn't have one!")
            logger.info("========================================")
            logger.info("")
        } catch (e: Exception) {
            logger.error("Error initializing legacy data", e)
        }
    }
}

