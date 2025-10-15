# Compensating Events Guide: Product Name Correction

## Overview

This guide demonstrates how to use **compensating events** in an event-sourced application to fix data quality issues without modifying historical events. This implementation shows how to correct bad product names in Order events while maintaining a complete audit trail.

## The Problem

In event sourcing, events are **immutable** - once written to the event store, they cannot be changed or deleted. This is a fundamental principle that ensures:
- Complete audit trail
- Event replay capability
- Historical accuracy

However, what happens when bad data gets into the event store? In this case, some `ItemAddedToOrder` events had incorrect or null `productName` fields due to an upstream validation issue.

## The Solution: Compensating Events

Instead of modifying historical events, we create a **compensating event** that corrects the state. This event represents the business fact: *"We discovered and corrected an incorrect product name."*

### Key Concepts

1. **Immutability**: Original events remain unchanged in the event store
2. **Audit Trail**: The correction itself is recorded as an event
3. **Event Replay**: When the aggregate is replayed, both the original event and the correction are applied
4. **Read Model Updates**: Event processors update read models when corrections are processed

## Implementation

### 1. The Compensating Event

```kotlin
data class OrderItemProductNameCorrected(
    val orderId: String,
    val productId: String,
    val oldProductName: String?,      // What it was (can be null)
    val correctedProductName: String,  // What it should be
    val correctedAt: Instant          // When it was corrected
)
```

This event contains:
- **Complete context**: All information needed to understand what happened
- **Old value**: Preserves what the incorrect value was
- **New value**: The corrected product name
- **Timestamp**: When the correction occurred

### 2. The Command

```kotlin
data class CorrectOrderItemProductName(
    @TargetAggregateIdentifier val orderId: String,
    val productId: String,
    val correctedProductName: String
)
```

This command is sent by an admin to trigger the correction.

### 3. Command Handler in Order Aggregate

```kotlin
@CommandHandler
fun handle(command: CorrectOrderItemProductName) {
    // Find the item with the specified productId
    val item = items.find { it.productId == command.productId }
        ?: throw IllegalArgumentException("Order item not found")

    // Validate the corrected name
    if (command.correctedProductName.isBlank()) {
        throw IllegalArgumentException("Corrected product name cannot be blank")
    }

    // Emit the compensating event
    AggregateLifecycle.apply(
        OrderItemProductNameCorrected(
            orderId = id,
            productId = command.productId,
            oldProductName = item.name,
            correctedProductName = command.correctedProductName,
            correctedAt = Instant.now()
        )
    )
}
```

### 4. Event Sourcing Handler in Order Aggregate

```kotlin
@EventSourcingHandler
fun on(event: OrderItemProductNameCorrected) {
    // Find the item and update its name
    val item = items.find { it.productId == event.productId }
    item?.let {
        it.name = event.correctedProductName
    }
}
```

**Critical**: This handler is called both:
- When the event is first published (updating current state)
- When the aggregate is replayed from the event store (rebuilding state)

### 5. Event Processor for Read Model

```kotlin
@EventHandler
fun on(event: OrderItemProductNameCorrected) {
    logger.info("Correcting product name from '${event.oldProductName}' to '${event.correctedProductName}'")

    orderRepository.findById(event.orderId).ifPresent { order ->
        // Update the MongoDB read model
        val updatedItems = order.items.map { item ->
            if (item.productId == event.productId) {
                item.copy(productName = event.correctedProductName)
            } else {
                item
            }
        }

        orderRepository.save(order.copy(items = updatedItems))
    }
}
```

### 6. Admin API Endpoint

```kotlin
@PostMapping("/admin/orders/{orderId}/items/{productId}/correct-name")
fun correctProductName(
    @PathVariable orderId: String,
    @PathVariable productId: String,
    @RequestBody request: CorrectProductNameRequest
): CompletableFuture<ResponseEntity<String>> {
    val command = CorrectOrderItemProductName(
        orderId = orderId,
        productId = productId,
        correctedProductName = request.correctedProductName
    )
    return commandGateway.send<String>(command)
        .thenApply { ResponseEntity.ok("Product name corrected successfully") }
}
```

## How It Works: Event Flow

### Initial State (Bad Data)
```
Event Store:
1. OrderCreated(orderId="order-1", customerId="customer-1")
2. ItemAddedToOrder(orderId="order-1", productId="prod-1", productName="Bad Name", ...)

Aggregate State:
- items = [OrderItem(productId="prod-1", name="Bad Name", ...)]

Read Model (MongoDB):
- OrderDocument(id="order-1", items=[OrderItemDocument(productId="prod-1", productName="Bad Name", ...)])
```

### After Correction
```
Event Store:
1. OrderCreated(orderId="order-1", customerId="customer-1")
2. ItemAddedToOrder(orderId="order-1", productId="prod-1", productName="Bad Name", ...)
3. OrderItemProductNameCorrected(orderId="order-1", productId="prod-1", 
                                  oldProductName="Bad Name", 
                                  correctedProductName="Espresso", ...)

Aggregate State (after replay):
- items = [OrderItem(productId="prod-1", name="Espresso", ...)]

Read Model (MongoDB):
- OrderDocument(id="order-1", items=[OrderItemDocument(productId="prod-1", productName="Espresso", ...)])
```

### Event Replay Demonstration

When the aggregate is loaded from the event store:

1. **OrderCreated** event is applied → Order aggregate created
2. **ItemAddedToOrder** event is applied → Item added with "Bad Name"
3. **OrderItemProductNameCorrected** event is applied → Name updated to "Espresso"

**Result**: The aggregate has the correct state, even though the original bad event is still in the event store!

## Usage

### Correcting a Product Name

```bash
# Correct a bad product name
curl -X POST http://localhost:8080/api/admin/orders/{orderId}/items/{productId}/correct-name \
  -H "Content-Type: application/json" \
  -d '{"correctedProductName": "Espresso"}'
```

### Example Scenarios

**Scenario 1: Null Product Name**
```bash
# Original event had productName = null or ""
POST /api/admin/orders/order-123/items/prod-456/correct-name
Body: {"correctedProductName": "Cappuccino"}
```

**Scenario 2: Incorrect Product Name**
```bash
# Original event had productName = "Wrong Name"
POST /api/admin/orders/order-789/items/prod-012/correct-name
Body: {"correctedProductName": "Latte"}
```

## Benefits

### 1. Complete Audit Trail
- Original bad data is preserved in the event store
- Correction is recorded as a separate event
- You can see exactly when and what was corrected

### 2. Event Replay Works Correctly
- Aggregate can be rebuilt from events at any time
- Corrections are automatically applied during replay
- No manual intervention needed

### 3. Read Model Consistency
- MongoDB read models are updated when correction events are processed
- Idempotency ensures corrections are applied exactly once
- No data loss or duplication

### 4. Business Semantics
- The correction is a business event, not a technical hack
- Clear intent: "We corrected a product name"
- Can be reported on, analyzed, etc.

## Testing

### Unit Tests
See `OrderCommandTests.kt` for examples:
- `should correct product name with compensating event()`
- `should correct null product name with compensating event()`
- `should apply correction during aggregate replay - demonstrating event sourcing()`

### Integration Tests
See `CoffeeShopApplicationTests.kt` for examples:
- `should correct product name using compensating event()`
- `should correct null product name using compensating event()`

## Key Takeaways

1. **Never modify historical events** - they are immutable facts
2. **Use compensating events** to correct state while preserving history
3. **Event sourcing handlers** must handle both new events and replays
4. **Read models** are updated through event processors
5. **Audit trail** is maintained automatically through the event store

## Related Patterns

- **Event Upcasting**: Transform events during deserialization (alternative approach)
- **Event Versioning**: Handle schema changes in events
- **Snapshots**: Optimize aggregate loading with many events

## References

- Event Sourcing: https://martinfowler.com/eaaDev/EventSourcing.html
- Axon Framework: https://docs.axoniq.io/
- Domain Events: https://martinfowler.com/eaaDev/DomainEvent.html

