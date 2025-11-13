# Domain Event Upgrade Analysis

## Problem Statement

The coffee-shop application is currently using **notification-style events** instead of proper **domain events** for an event-sourced CQRS application. Enhance the existing events rather than creating new ones. Ensure all tests pass and the data generator still works properly.

## Current State Analysis

### What are Notification-Style Events?

The current events are **notification-style** because they:
1. Only contain IDs/references (e.g., `OrderDelivered` only has `orderId`)
2. Require querying the aggregate or read model to get full state
3. Don't contain all the data needed to rebuild aggregate state independently

**Example of notification-style event:**
```kotlin
data class OrderDelivered(
    val orderId: String  // ❌ Only has ID - requires lookup to get order details
)
```

### What are Domain Events?

**Domain events** should:
1. Contain ALL data necessary to represent what happened
2. Be self-contained and immutable records of facts
3. Allow aggregate reconstruction without external queries
4. Capture the complete state change that occurred

**Example of proper domain event:**
```kotlin
data class OrderDelivered(
    val orderId: String,
    val customerId: String,
    val items: List<OrderItemData>,
    val totalAmount: Money,
    val deliveredAt: Instant  // ✅ Complete snapshot of what happened
)
```

## Event-by-Event Analysis

### Order Events

| Event | Status | Issues | Required Changes |
|-------|--------|--------|------------------|
| `OrderCreated` | ✅ Good | None | Already contains id, customerId |
| `ItemAddedToOrder` | ✅ Good | None | Already contains all item details |
| `OrderSubmitted` | ✅ Good | None | Already contains totalAmount |
| `OrderDelivered` | ❌ Notification | Only has orderId | Add: customerId, items, totalAmount, deliveredAt |
| `OrderCompleted` | ❌ Notification | Only has orderId | Add: customerId, items, totalAmount, deliveredAt, completedAt |

### Payment Events

| Event | Status | Issues | Required Changes |
|-------|--------|--------|------------------|
| `PaymentCreated` | ✅ Good | None | Already contains id, orderId, amount |
| `PaymentProcessed` | ❌ Notification | Only has paymentId, transactionId | Add: orderId, amount, processedAt |
| `PaymentFailed` | ❌ Notification | Only has paymentId, reason | Add: orderId, amount, failedAt |
| `PaymentRefunded` | ❌ Notification | Only has paymentId, refundId | Add: orderId, amount, refundedAt |
| `PaymentReset` | ❌ Notification | Only has paymentId | Add: orderId, amount, resetAt |

### Product Events

| Event | Status | Issues | Required Changes |
|-------|--------|--------|------------------|
| `ProductCreated` | ✅ Good | None | Already contains all product data |
| `ProductUpdated` | ✅ Good | None | Already contains all product data |
| `ProductDeleted` | ❌ Notification | Only has id | Add: name, description, price, deletedAt |

## Implementation Plan

### Phase 1: Order Events

#### 1.1 Update Order Event Definitions
- Enhance `OrderDelivered` with full order state
- Enhance `OrderCompleted` with full order state

#### 1.2 Update Order Aggregate
- Modify command handlers to include full state in events
- Update event sourcing handlers to use new event fields

#### 1.3 Update Order Event Processors
- Update event handlers to use new event fields
- Remove repository lookups (events should be self-contained)

#### 1.4 Update Order Tests
- Update unit tests with new event structures
- Update integration tests

### Phase 2: Payment Events

#### 2.1 Update Payment Event Definitions
- Enhance `PaymentProcessed` with full payment state
- Enhance `PaymentFailed` with full payment state
- Enhance `PaymentRefunded` with full payment state
- Enhance `PaymentReset` with full payment state

#### 2.2 Update Payment Aggregate
- Modify command handlers to include full state in events
- Update event sourcing handlers

#### 2.3 Update Payment Event Processors
- Update event handlers to use new event fields
- Remove repository lookups

#### 2.4 Update Payment Tests
- Update unit tests
- Update integration tests

### Phase 3: Product Events

#### 3.1 Update Product Event Definitions
- Enhance `ProductDeleted` with full product state

#### 3.2 Update Product Aggregate
- Modify DeleteProduct command handler
- Update event sourcing handler

#### 3.3 Update Product Event Processors
- Update event handlers

#### 3.4 Update Product Tests
- Update unit tests
- Update integration tests

### Phase 4: Data Generator Updates

#### 4.1 Verify DataGenerator
- No changes needed (sends commands, not events)
- Verify all generated data still works

### Phase 5: Integration & Verification

#### 5.1 Run All Tests
- Execute all unit tests
- Execute all integration tests
- Fix any failures

#### 5.2 Verify Event Store
- Ensure events are properly persisted
- Verify event replay works
- Test snapshot functionality

#### 5.3 Verify Read Models
- Ensure event processors work correctly
- Verify idempotency
- Test dead letter queue

## Key Principles for Domain Events

1. **Self-contained**: Events must contain all data needed to understand what happened
2. **Immutable**: Events represent historical facts and should never change
3. **Complete**: Include all relevant aggregate state at the time of the event
4. **Timestamped**: Include timestamps for when events occurred
5. **No lookups**: Event handlers should not need to query for additional data

## Benefits of Domain Events

- ✅ True event sourcing - can rebuild aggregate from events alone
- ✅ Better audit trail - events contain complete historical record
- ✅ Easier debugging - events are self-explanatory
- ✅ Better performance - no need to query for additional data in event handlers
- ✅ Proper CQRS - clear separation between command and query sides
- ✅ Event replay capability - can rebuild read models from scratch
- ✅ Time travel debugging - can see exact state at any point in time

## Migration Considerations

### Breaking Changes
- Event structure changes will affect:
  - Event store schema
  - Event processors
  - Any external consumers
  - Serialization/deserialization

### Backward Compatibility
- This is a breaking change to the event schema
- Existing events in the event store will need migration or versioning
- For this application (appears to be in development), we can proceed with direct changes

### Testing Strategy
1. Update all unit tests first
2. Update integration tests
3. Run full test suite
4. Manual verification of event sourcing and replay

