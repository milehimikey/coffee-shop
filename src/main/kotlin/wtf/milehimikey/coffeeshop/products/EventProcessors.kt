package wtf.milehimikey.coffeeshop.products

import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
@ProcessingGroup("product")
class ProductEventProcessor(private val productRepository: ProductRepository) {

    private val logger = LoggerFactory.getLogger(ProductEventProcessor::class.java)

    /**
     * Special product price that will trigger an error in the event processor.
     * Used to demonstrate dead letter queue functionality.
     */
    private val ERROR_TRIGGERING_PRICE = BigDecimal("99.99")

    @EventHandler
    fun on(event: ProductCreated) {
        logger.info("Processing ProductCreated event for product ${event.id}")
        
        // Check if this product should fail
        if (shouldFailProductProcessing(event.price)) {
            logger.error("Simulated error processing ProductCreated event for product ${event.id} with price $ERROR_TRIGGERING_PRICE")
            throw RuntimeException("Simulated error processing ProductCreated event for product with price $ERROR_TRIGGERING_PRICE")
        }
        
        productRepository.save(
            ProductDocument(
                id = event.id,
                name = event.name,
                description = event.description,
                price = event.price,
                active = true
            )
        )
    }

    @EventHandler
    fun on(event: ProductUpdated) {
        logger.info("Processing ProductUpdated event for product ${event.id}")
        
        // Check if this product should fail
        if (shouldFailProductProcessing(event.price)) {
            logger.error("Simulated error processing ProductUpdated event for product ${event.id} with price $ERROR_TRIGGERING_PRICE")
            throw RuntimeException("Simulated error processing ProductUpdated event for product with price $ERROR_TRIGGERING_PRICE")
        }
        
        productRepository.findById(event.id).ifPresent { product ->
            productRepository.save(
                product.copy(
                    name = event.name,
                    description = event.description,
                    price = event.price
                )
            )
        }
    }

    @EventHandler
    fun on(event: ProductDeleted) {
        logger.info("Processing ProductDeleted event for product ${event.id}")
        productRepository.findById(event.id).ifPresent { product ->
            productRepository.save(product.copy(active = false))
        }
    }
    
    /**
     * Helper method to determine if a product should fail processing based on its price.
     */
    private fun shouldFailProductProcessing(price: BigDecimal): Boolean {
        return price.compareTo(ERROR_TRIGGERING_PRICE) == 0
    }
}
