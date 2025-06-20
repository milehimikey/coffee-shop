package wtf.milehimikey.coffeeshop.orders

import org.axonframework.serialization.upcasting.event.IntermediateEventRepresentation
import org.springframework.stereotype.Component

/**
 * Upcaster for OrderSubmitted event to convert totalAmount from BigDecimal to Money.
 * This handles the migration from revision "1" to revision "2" of the OrderSubmitted event.
 */
@Component
class OrderSubmittedUpcaster : BaseMoneyUpcaster() {

    override fun canUpcast(intermediateRepresentation: IntermediateEventRepresentation): Boolean {
        return canUpcastEvent(
            intermediateRepresentation,
            "wtf.milehimikey.coffeeshop.orders.OrderSubmitted"
        )
    }

    override fun doUpcast(intermediateRepresentation: IntermediateEventRepresentation): IntermediateEventRepresentation {
        return upcastPayloadWithMoneyFields(
            intermediateRepresentation,
            listOf("totalAmount")
        )
    }
}
