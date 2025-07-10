package wtf.milehimikey.coffeeshop.admin

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Component responsible for initializing sample data when the application starts.
 * This uses the DataGenerator to create a batch of sample data.
 *
 * This component is excluded from the test profile to avoid creating sample data during tests.
 */
@Component
@Profile("!test") // Don't run in test profile
class DataInitializer(private val dataGenerator: DataGenerator) {
    private val logger = LoggerFactory.getLogger(DataInitializer::class.java)

//    @EventListener(ApplicationStartedEvent::class)
    fun initialize() {
        logger.info("Initializing sample data...")

        try {
            // Generate a batch of sample data
            val result = dataGenerator.generateBatch(
                productCount = 15,      // Create 15 products
                orderCount = 300,        // Create 300 orders
                triggerSnapshots = false, // Trigger snapshots for all aggregate types
                triggerDeadLetters = false // Trigger dead letters for all processors
            )

            logger.info("Sample data initialization completed successfully")
            logger.info("Generated ${result.productCount} products, ${result.orderCount} orders, and ${result.paymentCount} payments")
            logger.info("Snapshots triggered: ${result.snapshotsTriggered}, Dead letters triggered: ${result.deadLettersTriggered}")
        } catch (e: Exception) {
            logger.error("Error initializing sample data", e)
        }
    }
}
