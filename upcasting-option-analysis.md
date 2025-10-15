# Event Upcasting Analysis: Adding SKU Field to Products

## Problem Statement

Products need to have a `sku` field which cannot be null, but all existing products don't have a SKU on them. This is a common schema evolution challenge in event-sourced systems.

## Solution: Event Upcasting with Multi-Strategy Fallback

We are implementing **Option 1C: Multi-Strategy Upcaster with Fallback Chain** which provides maximum flexibility while maintaining event immutability.

---

## Explored Solutions

### **Solution 1A: Upcaster with External Lookup Table**

The upcaster reads from a lookup table (populated from CSV) to map product IDs to SKUs.

**Pros:**
- ✅ Uses real business SKUs from product team
- ✅ Transparent to aggregate - always sees SKU
- ✅ Can update CSV and restart app if corrections needed
- ✅ Fallback strategy for missing mappings

**Cons:**
- ❌ Requires CSV to be deployed with application
- ❌ Lookup service must be fast (consider caching)
- ❌ Need fallback strategy for products not in CSV

---

### **Solution 1B: Hybrid Upcaster with Database Lookup**

Instead of CSV in memory, store the mapping in a database table that can be updated.

**Pros:**
- ✅ Can update mappings without redeploying
- ✅ Cached for performance
- ✅ Can query/audit mappings
- ✅ Supports corrections and updates

**Cons:**
- ❌ Adds database dependency to upcasting
- ❌ Need to ensure database is available during event replay
- ❌ More complex infrastructure

---

### **Solution 1C: Multi-Strategy Upcaster with Fallback Chain** ⭐ **SELECTED**

Combine multiple strategies with a fallback chain for maximum flexibility.

**Strategy Chain:**
1. **CSV Lookup** - Primary source from product team
2. **Name-based Generation** - Generate SKU from product name (e.g., "Espresso" -> "ESP-LEGACY")
3. **ID-based Generation** - Fallback using product ID (e.g., "PROD-{id}")

**Pros:**
- ✅ Maximum flexibility - multiple data sources
- ✅ Graceful degradation if CSV missing
- ✅ Can use business logic (name-based SKU generation)
- ✅ Always produces a SKU
- ✅ Deterministic - same input always produces same SKU
- ✅ No migration scripts needed

**Cons:**
- ❌ More complex logic
- ❌ Need to document fallback strategy clearly

---

### **Solution 1D: Event Revision with Upcasting**

Use Axon's event revision mechanism to version events and upcast between versions.

**Pros:**
- ✅ Explicit versioning of events
- ✅ Clear migration path
- ✅ Axon framework native approach
- ✅ Can track which events have been upcasted

**Cons:**
- ❌ More ceremony with revision annotations
- ❌ Still need lookup mechanism

---

### **Solution 1E: Lazy Upcasting with Admin Tool**

Provide an admin endpoint to "enrich" the SKU mapping over time.

**Pros:**
- ✅ Can correct mappings without redeployment
- ✅ Product team can upload new CSVs via API
- ✅ Flexible and maintainable
- ✅ Audit trail of changes

**Cons:**
- ❌ Requires admin tooling
- ❌ Need to persist dynamic mappings

---

## Selected Implementation: Solution 1C

### Why This Approach?

1. **Handles CSV from product team** - Primary source of truth
2. **Graceful fallbacks** - Won't break if CSV is incomplete
3. **Deterministic** - Same product ID always gets same SKU
4. **No migration scripts** - Works automatically during event replay
5. **Flexible** - Can add database lookup later if needed
6. **Event immutability preserved** - Historical events remain unchanged

### Architecture Overview

```
Event Store (Old Events without SKU)
         ↓
    Upcaster (reads event)
         ↓
    SkuLookupService
         ↓
    Strategy Chain:
    1. CSV Lookup
    2. Name-based Generation
    3. ID-based Generation
         ↓
    Event with SKU added
         ↓
    Aggregate (always sees SKU)
```

### Components

1. **ProductCreatedUpcaster** - Axon upcaster that intercepts old events
2. **SkuLookupService** - Service that manages SKU lookup strategies
3. **CSV File** - `src/main/resources/sku-mappings.csv` with product_id,sku mappings
4. **Fallback Generators** - Name-based and ID-based SKU generation

### CSV Format

```csv
product_id,sku
product-1,ESP-001
product-2,LAT-001
product-3,CAP-001
```

### Event Evolution

**Old Event (stored in event store):**
```kotlin
ProductCreated(
    id = "product-1",
    name = "Espresso",
    description = "Strong coffee",
    price = Money.of(3.50, "USD")
    // No SKU field
)
```

**Upcasted Event (what aggregate sees):**
```kotlin
ProductCreated(
    id = "product-1",
    name = "Espresso",
    description = "Strong coffee",
    price = Money.of(3.50, "USD"),
    sku = "ESP-001"  // Added by upcaster from CSV
)
```

**New Event (created going forward):**
```kotlin
ProductCreated(
    id = "product-4",
    name = "Mocha",
    description = "Chocolate coffee",
    price = Money.of(4.50, "USD"),
    sku = "MOC-001"  // Included in command/event
)
```

### Fallback Strategy Examples

| Product ID | Name | CSV Match? | SKU Result | Strategy Used |
|------------|------|------------|------------|---------------|
| product-1 | Espresso | ✅ Yes | ESP-001 | CSV Lookup |
| product-2 | Latte | ❌ No | LAT-LEGACY | Name-based |
| product-3 | Unknown | ❌ No | PROD-product-3 | ID-based |

### Benefits

- ✅ **True Event Sourcing** - Can rebuild aggregate from events alone
- ✅ **No Data Migration** - Works automatically during event replay
- ✅ **Immutable Events** - Historical events never modified
- ✅ **Transparent** - Aggregate code doesn't know about upcasting
- ✅ **Testable** - Can test upcasting logic independently
- ✅ **Flexible** - Easy to add new strategies or update CSV

### Implementation Steps

1. Create `SkuLookupService` to manage lookup strategies
2. Create `ProductCreatedUpcaster` to upcast old events
3. Add CSV file with product ID to SKU mappings
4. Update `ProductCreated` event to include optional `sku` field
5. Update `CreateProduct` command to require `sku` field
6. Update `Product` aggregate to handle `sku` field
7. Update event sourcing handler to apply `sku`
8. Update read models (ProductDocument, ProductView) to include `sku`
9. Write tests for upcasting logic
10. Test with existing event store data

### Testing Strategy

1. **Unit Tests** - Test upcaster with various scenarios
2. **Integration Tests** - Test event replay with old events
3. **Fallback Tests** - Verify each fallback strategy works
4. **CSV Loading Tests** - Test CSV parsing and lookup
5. **Aggregate Tests** - Verify aggregate handles SKU correctly

---

## Alternative Approaches Considered (Not Selected)

### Option 2: Compensating Events
Create a `ProductSkuAssigned` event and run migration script.
- **Rejected because:** Requires migration script and temporary nullable handling

### Option 3: Nullable Field with Default
Keep SKU nullable in events but non-nullable in aggregate.
- **Rejected because:** Aggregate logic becomes more complex

### Option 4: Snapshot-Based Migration
Create snapshots for all existing aggregates with generated SKUs.
- **Rejected because:** Requires snapshot infrastructure and migration script

---

## References

- Axon Framework Event Upcasting: https://docs.axoniq.io/reference-guide/axon-framework/events/event-versioning#event-upcasting
- Event Sourcing Schema Evolution: https://martinfowler.com/articles/201701-event-driven.html
- Domain Events: https://martinfowler.com/eaaDev/DomainEvent.html

