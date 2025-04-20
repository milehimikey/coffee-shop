package wtf.milehimikey.coffeeshop.products

import java.math.BigDecimal

data class ProductCreated(
    val id: String,
    val name: String,
    val description: String,
    val price: BigDecimal
)

data class ProductUpdated(
    val id: String,
    val name: String,
    val description: String,
    val price: BigDecimal
)

data class ProductDeleted(
    val id: String
)
