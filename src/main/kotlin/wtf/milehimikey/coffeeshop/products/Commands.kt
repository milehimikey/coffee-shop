package wtf.milehimikey.coffeeshop.products

import org.axonframework.modelling.annotation.TargetEntityId
import org.javamoney.moneta.Money
import java.util.*

data class CreateProduct(
    @TargetEntityId val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val price: Money,
    val sku: String
)

data class CreateLegacyProduct(
    @TargetEntityId val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val price: Money
)

data class UpdateProduct(
    @TargetEntityId val id: String,
    val name: String,
    val description: String,
    val price: Money
)

data class DeleteProduct(
    @TargetEntityId val id: String
)
