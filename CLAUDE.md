# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run application
./gradlew bootRun

# Run with legacy data (upcaster demo)
./gradlew bootRun --args='--spring.profiles.active=legacy-data'

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "wtf.milehimikey.coffeeshop.products.ProductCommandTests"

# Run a single test method
./gradlew test --tests "wtf.milehimikey.coffeeshop.products.ProductCommandTests.should create product"

# Build Docker image
./gradlew jibDockerBuild
```

Infrastructure (MongoDB + PostgreSQL) is started automatically via Spring Boot Docker Compose integration when running locally. Tests use Testcontainers with `@ServiceConnection`.

## Architecture

This is an **event-sourced CQRS** application using Axon Framework 4.12 on Spring Boot 3.5 with Kotlin.

- **Event Store**: PostgreSQL (via Axon's JPA event storage)
- **Read Models**: MongoDB projections
- **Serialization**: Jackson (JSON)

### Three Bounded Contexts

Each context lives in its own package under `wtf.milehimikey.coffeeshop/` and follows the same file layout:

| File | Purpose |
|------|---------|
| `Commands.kt` | Command data classes with `@TargetAggregateIdentifier` |
| `Events.kt` | Immutable event data classes |
| `Queries.kt` | Query data classes + `@QueryHandler` implementations |
| `{Aggregate}.kt` | Aggregate root with `@CommandHandler` and `@EventSourcingHandler` |
| `EventProcessors.kt` | Projection `@EventHandler` methods that update MongoDB |
| `Repositories.kt` | MongoDB repository interface + document data class |

**Products** — SKU-based catalog with upcasting support for legacy events  
**Orders** — State machine: `NEW → SUBMITTED → DELIVERED → COMPLETED`, supports compensating events for product name corrections  
**Payments** — State machine: `PENDING → PROCESSED/FAILED → REFUNDED`

### Key Infrastructure (config/ package)

- **`AxonConfig.kt`** — Snapshot trigger beans (Order: 50 events, Product: 200, Payment: 25), DLQ configuration
- **`IdempotencyInterceptor.kt`** — Exactly-once event processing tracked via PostgreSQL JPA
- **`DeadLetterProcessor.kt`** — Scheduled and manual DLQ retry handling
- **`JacksonConfiguration.kt`** — Serialization setup for Axon events

### Event Upcasting

`ProductCreatedUpcaster` transforms legacy `ProductCreated` events (missing the `sku` field) using a multi-strategy lookup: CSV file → name-based → ID-based. Operates on raw `JsonNode` before deserialization.

### REST API

All endpoints are in `RestEndpoint.kt`. The admin UI (Thymeleaf) lives in `admin/` and includes a data generator and dashboard. A custom actuator endpoint at `/actuator/deadletters` exposes DLQ state.

## Testing

**Aggregate tests** use `AggregateTestFixture` with `given/when/expect` DSL:

```kotlin
fixture.givenNoPriorActivity()
    .`when`(CreateProduct(...))
    .expectSuccessfulHandlerExecution()
    .expectEvents(ProductCreated(...))
```

**Integration tests** (`CoffeeShopApplicationTests`) use `TestRestTemplate` + Testcontainers + Awaitility for async event processing assertions. Each integration test class is annotated `@DirtiesContext`.

Event payload matching uses `payloadsMatching(exactSequenceOf(...))` with Hamcrest predicates.

## Axon Patterns Used

- `@Aggregate` with `@AggregateIdentifier`
- `@CommandHandler` on constructor (creation) and methods (updates)
- `@EventSourcingHandler` to rebuild aggregate state
- `EventCountSnapshotTriggerDefinition` for performance
- Pooled streaming event processors (8 threads, batch size 10) configured in `application.yml`
- Dead Letter Queue per processor with configurable retry
- Compensating events (not updating history) for corrections
