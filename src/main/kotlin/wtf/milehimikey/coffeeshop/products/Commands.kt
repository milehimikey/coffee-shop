package wtf.milehimikey.coffeeshop.products

import org.axonframework.modelling.command.TargetAggregateIdentifier
import org.javamoney.moneta.Money
import java.util.*

data class CreateProduct(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val price: Money
)

data class UpdateProduct(
    @TargetAggregateIdentifier val id: String,
    val name: String,
    val description: String,
    val price: Money
)

data class DeleteProduct(
    @TargetAggregateIdentifier val id: String
)
