package wtf.milehimikey.coffeeshop.orders

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.axonframework.serialization.SimpleSerializedType
import org.axonframework.serialization.upcasting.event.IntermediateEventRepresentation
import org.axonframework.serialization.upcasting.event.SingleEventUpcaster

/**
 * Base class for upcasters that convert BigDecimal fields to Money objects.
 * Provides common functionality for converting BigDecimal values to Money JSON structure.
 */
abstract class BaseMoneyUpcaster : SingleEventUpcaster() {

    companion object {
        const val DEFAULT_CURRENCY = "USD"
        const val OLD_REVISION = "1"
        const val NEW_REVISION = "2"
    }

    /**
     * Common method to upcast a payload by converting specified BigDecimal fields to Money objects.
     */
    protected fun upcastPayloadWithMoneyFields(
        intermediateRepresentation: IntermediateEventRepresentation,
        fieldNames: List<String>
    ): IntermediateEventRepresentation {
        return intermediateRepresentation.upcastPayload(
            SimpleSerializedType(
                intermediateRepresentation.type.name,
                NEW_REVISION
            ),
            JsonNode::class.java
        ) { jsonNode ->
            convertBigDecimalFieldsToMoney(jsonNode, fieldNames)
        }
    }

    /**
     * Converts specified BigDecimal fields to Money object structure in the JSON node.
     */
    protected fun convertBigDecimalFieldsToMoney(jsonNode: JsonNode, fieldNames: List<String>): JsonNode {
        val objectNode = jsonNode as ObjectNode

        fieldNames.forEach { fieldName ->
            val fieldNode = objectNode.get(fieldName)
            if (fieldNode != null && fieldNode.isNumber) {
                val fieldValue = fieldNode.decimalValue()
                val moneyObject = objectNode.objectNode().apply {
                    put("amount", fieldValue)
                    put("currency", DEFAULT_CURRENCY)
                }
                objectNode.set<JsonNode>(fieldName, moneyObject)
            }
        }

        return objectNode
    }

    /**
     * Helper method to check if an event can be upcasted based on event name and revision.
     */
    protected fun canUpcastEvent(
        intermediateRepresentation: IntermediateEventRepresentation,
        eventClassName: String
    ): Boolean {
        return intermediateRepresentation.type.name == eventClassName &&
                (intermediateRepresentation.type.revision == null || intermediateRepresentation.type.revision == OLD_REVISION)
    }
}
