package wtf.milehimikey.coffeeshop.products

import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component

@Component
@ProcessingGroup("product")
class ProductEventProcessor(private val productRepository: ProductRepository) {

    @EventHandler
    fun on(event: ProductCreated) {
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
        productRepository.findById(event.id).ifPresent { product ->
            productRepository.save(product.copy(active = false))
        }
    }
}
