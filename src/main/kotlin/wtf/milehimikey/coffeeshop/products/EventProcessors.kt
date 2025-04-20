package wtf.milehimikey.coffeeshop.products

import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@ProcessingGroup("product")
class ProductEventProcessor(private val productRepository: ProductRepository) {

    @EventHandler
    @Transactional
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
    @Transactional
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
    @Transactional
    fun on(event: ProductDeleted) {
        productRepository.findById(event.id).ifPresent { product ->
            productRepository.save(product.copy(active = false))
        }
    }
}
