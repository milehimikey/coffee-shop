package wtf.milehimikey.coffeeshop.products

import org.javamoney.moneta.Money
import java.time.Instant

data class ProductCreated(
    val id: String,
    val name: String,
    val description: String,
    val price: Money
)

data class ProductUpdated(
    val id: String,
    val name: String,
    val description: String,
    val price: Money
)

/**
 * Domain event representing a product being deleted.
 * Contains complete product state for proper event sourcing.
 */
data class ProductDeleted(
    val id: String,
    val name: String,
    val description: String,
    val price: Money,
    val deletedAt: Instant
)
