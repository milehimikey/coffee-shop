package wtf.milehimikey.coffeeshop.products

import org.axonframework.modelling.command.TargetAggregateIdentifier
import org.javamoney.moneta.Money
import java.util.*

data class CreateProduct(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val price: Money,
    val sku: String
)

/**
 * Command to create a "legacy" product without a SKU field.
 * This is used to demonstrate the ProductCreatedUpcaster functionality.
 *
 * When this command is handled, it will create a ProductCreated event WITHOUT
 * a SKU field, simulating products that existed before the SKU field was added.
 * The ProductCreatedUpcaster will add the SKU field when the event is replayed.
 */
data class CreateLegacyProduct(
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
