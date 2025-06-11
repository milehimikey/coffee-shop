package wtf.milehimikey.coffeeshop.orders

import org.axonframework.serialization.upcasting.event.IntermediateEventRepresentation
import org.springframework.stereotype.Component

/**
 * Upcaster for ItemAddedToOrder event to convert price from BigDecimal to Money.
 * This handles the migration from revision "1" to revision "2" of the ItemAddedToOrder event.
 */
@Component
class ItemAddedToOrderUpcaster : BaseMoneyUpcaster() {

    override fun canUpcast(intermediateRepresentation: IntermediateEventRepresentation): Boolean {
        return canUpcastEvent(
            intermediateRepresentation,
            "wtf.milehimikey.coffeeshop.orders.ItemAddedToOrder"
        )
    }

    override fun doUpcast(intermediateRepresentation: IntermediateEventRepresentation): IntermediateEventRepresentation {
        return upcastPayloadWithMoneyFields(
            intermediateRepresentation,
            listOf("price")
        )
    }
}
