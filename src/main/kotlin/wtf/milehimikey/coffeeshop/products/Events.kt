package wtf.milehimikey.coffeeshop.products

import org.axonframework.eventsourcing.annotation.EventTag
import org.javamoney.moneta.Money
import java.time.Instant

data class ProductCreated(
    @EventTag(key = "id") val id: String,
    val name: String,
    val description: String,
    val price: Money,
    val sku: String? = null
)

data class ProductUpdated(
    @EventTag(key = "id") val id: String,
    val name: String,
    val description: String,
    val price: Money
)

data class ProductDeleted(
    @EventTag(key = "id") val id: String,
    val name: String,
    val description: String,
    val price: Money,
    val deletedAt: Instant
)
