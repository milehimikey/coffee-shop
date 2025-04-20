package wtf.milehimikey.coffeeshop.products

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.math.BigDecimal
import java.util.*

data class CreateProduct(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val price: BigDecimal
)

data class UpdateProduct(
    @TargetAggregateIdentifier val id: String,
    val name: String,
    val description: String,
    val price: BigDecimal
)

data class DeleteProduct(
    @TargetAggregateIdentifier val id: String
)
