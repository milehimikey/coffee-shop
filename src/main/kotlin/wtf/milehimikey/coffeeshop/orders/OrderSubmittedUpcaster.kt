package wtf.milehimikey.coffeeshop.orders

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.axonframework.serialization.SimpleSerializedType
import org.axonframework.serialization.upcasting.event.IntermediateEventRepresentation
import org.axonframework.serialization.upcasting.event.SingleEventUpcaster
import org.springframework.stereotype.Component

/**
 * Upcaster for OrderSubmitted event to convert totalAmount from BigDecimal to Money.
 * This handles the migration from revision "1" to revision "2" of the OrderSubmitted event.
 */
@Component
class OrderSubmittedUpcaster : SingleEventUpcaster() {

    companion object {
        private const val DEFAULT_CURRENCY = "USD"
        private const val OLD_REVISION = "1"
        private const val NEW_REVISION = "2"
    }

    override fun canUpcast(intermediateRepresentation: IntermediateEventRepresentation): Boolean {
        return intermediateRepresentation.type.name == "wtf.milehimikey.coffeeshop.orders.OrderSubmitted" &&
                (intermediateRepresentation.type.revision == null || intermediateRepresentation.type.revision == OLD_REVISION)
    }

    override fun doUpcast(intermediateRepresentation: IntermediateEventRepresentation): IntermediateEventRepresentation {
        return intermediateRepresentation.upcastPayload(
            SimpleSerializedType(
                intermediateRepresentation.type.name,
                NEW_REVISION
            ),
            JsonNode::class.java
        ) { jsonNode ->
            upcastOrderSubmittedPayload(jsonNode)
        }
    }

    private fun upcastOrderSubmittedPayload(jsonNode: JsonNode): JsonNode {
        val objectNode = jsonNode as ObjectNode

        // Get the current totalAmount as BigDecimal
        val totalAmountNode = objectNode.get("totalAmount")

        if (totalAmountNode != null && totalAmountNode.isNumber) {
            val totalAmountValue = totalAmountNode.decimalValue()

            // Create Money object structure
            val moneyObject = objectNode.objectNode().apply {
                put("amount", totalAmountValue)
                put("currency", DEFAULT_CURRENCY)
            }

            // Replace the totalAmount field with the Money structure
            objectNode.set<JsonNode>("totalAmount", moneyObject)
        }

        return objectNode
    }
}
