package wtf.milehimikey.coffeeshop.products

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.spring.stereotype.Aggregate
import java.math.BigDecimal
import java.util.*

@Aggregate
class Product {

    @AggregateIdentifier
    private lateinit var id: String
    private lateinit var name: String
    private lateinit var description: String
    private lateinit var price: BigDecimal
    private var active: Boolean = true

    constructor()  // Required by Axon

    @CommandHandler
    constructor(command: CreateProduct) {
        AggregateLifecycle.apply(
            ProductCreated(
                id = command.id,
                name = command.name,
                description = command.description,
                price = command.price
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
            ProductDeleted(id = command.id)
        )
    }

    @EventSourcingHandler
    fun on(event: ProductCreated) {
        id = event.id
        name = event.name
        description = event.description
        price = event.price
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
