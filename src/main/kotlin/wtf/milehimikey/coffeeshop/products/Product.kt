package wtf.milehimikey.coffeeshop.products

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.spring.stereotype.Aggregate
import org.javamoney.moneta.Money
import java.time.Instant

@Aggregate(snapshotTriggerDefinition = "productSnapshotTriggerDefinition")
class Product {

    @AggregateIdentifier
    lateinit var id: String
    private lateinit var name: String
    private lateinit var description: String
    private lateinit var price: Money
    private lateinit var sku: String
    private var active: Boolean = true

    constructor()  // Required by Axon

    @CommandHandler
    constructor(command: CreateProduct) {
        AggregateLifecycle.apply(
            ProductCreated(
                id = command.id,
                name = command.name,
                description = command.description,
                price = command.price,
                sku = command.sku
            )
        )
    }

    @CommandHandler
    fun handle(command: UpdateProduct) {
        if (!active) {
            throw IllegalStateException("Cannot update a deleted product")
        }

        AggregateLifecycle.apply(
            ProductUpdated(
                id = command.id,
                name = command.name,
                description = command.description,
                price = command.price
            )
        )
    }

    @CommandHandler
    fun handle(command: DeleteProduct) {
        if (!active) {
            throw IllegalStateException("Product is already deleted")
        }

        AggregateLifecycle.apply(
            ProductDeleted(
                id = id,
                name = name,
                description = description,
                price = price,
                deletedAt = Instant.now()
            )
        )
    }

    @EventSourcingHandler
    fun on(event: ProductCreated) {
        id = event.id
        name = event.name
        description = event.description
        price = event.price
        // SKU will be provided by upcaster for old events, or directly from new events
        sku = event.sku ?: throw IllegalStateException("ProductCreated event missing SKU field")
        active = true
    }

    @EventSourcingHandler
    fun on(event: ProductUpdated) {
        name = event.name
        description = event.description
        price = event.price
    }

    @EventSourcingHandler
    fun on(event: ProductDeleted) {
        active = false
    }
}
