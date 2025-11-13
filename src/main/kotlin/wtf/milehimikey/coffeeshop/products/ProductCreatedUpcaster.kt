package wtf.milehimikey.coffeeshop.products

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.axonframework.serialization.upcasting.event.IntermediateEventRepresentation
import org.axonframework.serialization.upcasting.event.SingleEventUpcaster
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Event upcaster that adds the SKU field to ProductCreated events that don't have one.
 *
 * This upcaster is part of the event sourcing schema evolution strategy. It intercepts
 * old ProductCreated events during deserialization and adds a SKU field using the
 * SkuLookupService's multi-strategy fallback approach.
 *
 * The upcaster is transparent to the aggregate - the Product aggregate always sees
 * ProductCreated events with a SKU field, regardless of whether the event was created
 * before or after the SKU field was added.
 *
 * Strategy:
 * 1. Check if event is ProductCreated and doesn't have a SKU field
 * 2. Extract product ID and name from the event
 * 3. Use SkuLookupService to get SKU (CSV lookup -> name-based -> ID-based)
 * 4. Add SKU field to the event
 *
 * @see SkuLookupService for details on SKU lookup strategies
 */
@Component
class ProductCreatedUpcaster(
    private val skuLookupService: SkuLookupService
) : SingleEventUpcaster() {

    private val logger = LoggerFactory.getLogger(ProductCreatedUpcaster::class.java)

    companion object {
        private const val PRODUCT_CREATED_EVENT_TYPE = "wtf.milehimikey.coffeeshop.products.ProductCreated"
    }

    /**
     * Determines if this upcaster can handle the given event.
     *
     * Returns true if:
     * - The event type is ProductCreated
     * - The event does not have a "sku" field OR the sku field is null
     *
     * @param intermediateRepresentation The event representation
     * @return true if this upcaster should process the event
     */
    override fun canUpcast(intermediateRepresentation: IntermediateEventRepresentation): Boolean {
        val eventType = intermediateRepresentation.type

        // Only upcast ProductCreated events
        if (eventType.name != PRODUCT_CREATED_EVENT_TYPE) {
            return false
        }

        // Check if the event already has a non-null SKU field
        val data = intermediateRepresentation.data as? JsonNode ?: return false
        val hasSku = data.has("sku") && !data.get("sku").isNull

        // Upcast if SKU is missing or null
        return !hasSku
    }

    /**
     * Performs the upcasting by adding the SKU field to the event.
     *
     * @param intermediateRepresentation The event representation to upcast
     * @return The upcasted event representation with SKU field added
     */
    override fun doUpcast(intermediateRepresentation: IntermediateEventRepresentation): IntermediateEventRepresentation {
        val data = intermediateRepresentation.data as ObjectNode

        // Extract product ID and name from the event
        val productId = data.get("id")?.asText() ?: run {
            logger.error("ProductCreated event missing 'id' field, cannot upcast")
            return intermediateRepresentation
        }

        val productName = data.get("name")?.asText()

        // Get SKU using the lookup service
        val sku = skuLookupService.getSkuForProduct(productId, productName)

        logger.info("Upcasting ProductCreated event for product $productId: adding SKU = $sku")

        // Add the SKU field to the event data (modifies in place)
        data.put("sku", sku)

        // Return the same intermediate representation (data was modified in place)
        return intermediateRepresentation
    }
}

