package wtf.milehimikey.coffeeshop.products

import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.javamoney.moneta.Money
import java.time.Instant

@EventSourced(tagKey = "id")
class Product {

    private lateinit var id: String
    private lateinit var name: String
    private lateinit var description: String
    private lateinit var price: Money
    private lateinit var sku: String
    private var active: Boolean = true

    companion object {
        @JvmStatic
        @CommandHandler
        fun create(command: CreateProduct, appender: EventAppender) {
            appender.append(
                ProductCreated(
                    id = command.id,
                    name = command.name,
                    description = command.description,
                    price = command.price,
                    sku = command.sku
                )
            )
        }

        @JvmStatic
        @CommandHandler
        fun createLegacy(command: CreateLegacyProduct, appender: EventAppender) {
            appender.append(
                ProductCreated(
                    id = command.id,
                    name = command.name,
                    description = command.description,
                    price = command.price,
                    sku = null
                )
            )
        }
    }

    @EntityCreator
    constructor(event: ProductCreated) {
        id = event.id
        name = event.name
        description = event.description
        price = event.price
        sku = event.sku ?: "LEGACY-PENDING-${event.id.take(8)}"
        active = true
    }

    @CommandHandler
    fun handle(command: UpdateProduct, appender: EventAppender) {
        if (!active) {
            throw IllegalStateException("Cannot update a deleted product")
        }
        appender.append(
            ProductUpdated(
                id = command.id,
                name = command.name,
                description = command.description,
                price = command.price
            )
        )
    }

    @CommandHandler
    fun handle(command: DeleteProduct, appender: EventAppender) {
        if (!active) {
            throw IllegalStateException("Product is already deleted")
        }
        appender.append(
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
