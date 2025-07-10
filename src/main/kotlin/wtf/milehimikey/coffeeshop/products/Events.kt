package wtf.milehimikey.coffeeshop.products

import org.javamoney.moneta.Money

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

data class ProductDeleted(
    val id: String
)
