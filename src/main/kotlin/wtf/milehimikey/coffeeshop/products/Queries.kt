package wtf.milehimikey.coffeeshop.products

import org.axonframework.config.ProcessingGroup
import org.axonframework.queryhandling.QueryHandler
import org.javamoney.moneta.Money
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

// Queries
data class FindProductById(val id: String)
data class FindAllProducts(val includeInactive: Boolean = false)

// Views
data class ProductView(
    val id: String,
    val name: String,
    val description: String,
    val price: Money,
    val active: Boolean
)

// Query Handlers
@Component
@ProcessingGroup("product")
class ProductQueryHandler(private val productRepository: ProductRepository) {

    @QueryHandler
    @Transactional(readOnly = true)
    fun handle(query: FindProductById): ProductView? {
        return productRepository.findById(query.id)
            .map { it.toView() }
            .orElse(null)
    }

    @QueryHandler
    @Transactional(readOnly = true)
    fun handle(query: FindAllProducts): List<ProductView> {
        return if (query.includeInactive) {
            productRepository.findAll().map { it.toView() }
        } else {
            productRepository.findByActiveTrue().map { it.toView() }
        }
    }

    private fun ProductDocument.toView(): ProductView {
        return ProductView(
            id = this.id,
            name = this.name,
            description = this.description,
            price = this.price,
            active = this.active
        )
    }
}
