# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run application (starts MongoDB + PostgreSQL + Axon Server via compose)
./gradlew bootRun

# Run with legacy data (products whose events carry no SKU)
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

Infrastructure starts automatically via Spring Boot Docker Compose integration. Tests use Testcontainers with `@ServiceConnection`.

## Licensing constraint — read before running

The `io.axoniq.framework` artifacts are under the [AxonIQ Terms of Service](https://www.axoniq.io/legal/terms-of-service), **not** Apache 2.0. They enforce licensing at runtime:

```
[AXONIQ] No license is available for this application.
The JVM will shut down after a 15-minute grace period if no valid license is found.
```

Without a license the app runs in 15-minute windows. Tests finish well inside that, so the suite is unaffected. The Sequenced Dead-Letter Queue (`framework.dead_letter_queue`) is a separately licensed addon — enabling `axon.eventhandling.processors.*.dlq.enabled` starts the shutdown timer and does not capture events without a license. Use the custom DLQ instead (see "Dead letter handling").

## Architecture

Event-sourced CQRS on **Axon Framework 5.2.0**, Spring Boot 3.5, Kotlin.

| Concern | Technology |
|---|---|
| Event store, command bus, query bus | Axon Server (DCB context, gRPC) |
| Operational read models | MongoDB projections |
| Reporting read model | PostgreSQL via JPA |
| Tracking tokens | PostgreSQL (`token_entry`) |
| Custom dead-letter store | PostgreSQL (`failed_events`) |
| Idempotency records | MongoDB (`idempotency_records`) |
| Serialization | Jackson 2 via `Jackson2Converter` |

Command and query buses are **distributed** — they round-trip through Axon Server over gRPC, not in-process.

### Bounded contexts

Each lives in its own package under `wtf.milehimikey.coffeeshop/` with the same layout:

| File | Purpose |
|---|---|
| `Commands.kt` | Command data classes with `@TargetEntityId` |
| `Events.kt` | Immutable event data classes with `@EventTag` |
| `Queries.kt` | Query data classes + `@QueryHandler` implementations |
| `{Entity}.kt` | `@EventSourced` entity with `@CommandHandler`, `@EntityCreator`, `@EventSourcingHandler` |
| `EventProcessors.kt` | Projection `@EventHandler` methods that update MongoDB |
| `Repositories.kt` | MongoDB repository interface + document data class |

**Products** — SKU-based catalog
**Orders** — `NEW → SUBMITTED → DELIVERED → COMPLETED`, with compensating events for product name corrections
**Payments** — `PENDING → PROCESSED/FAILED → REFUNDED`
**Reporting** — daily revenue rollup on PostgreSQL (`reporting/`), fed by `OrderCompleted` and `PaymentProcessed`

### Axon 5 patterns

- `@EventSourced(tagKey = "...")` on the entity; matching `@EventTag(key = "...")` on every event. This is what makes the model DCB-native.
- Creation commands are `@JvmStatic @CommandHandler` functions on the `companion object`, taking `(command, appender: EventAppender)`.
- **Creation handlers must return the identifier.** Unlike Axon 4's aggregate constructor, the `EventAppender` style returns nothing implicitly, so `commandGateway.send(command, String::class.java)` yields null and REST endpoints return 200 with an empty body.
- `@EntityCreator` constructor seeds state; `@EventSourcingHandler` methods mutate it.
- Compensating events (never rewriting history) for corrections.

## Key infrastructure (config/)

- **`AxonConfig.kt`** — one `EventProcessorDefinition` per processing group (`order`, `payment`, `product`, `reporting`), each wiring the custom error handler.
  - **Never use `EventHandlerSelector.matchesNamespaceOnType` to select by package.** Despite taking a package-shaped string it matches a `namespace` *attribute* on an annotation on the handler type, not the Java package. Nothing here declares one, so it silently matches nothing and the whole definition becomes inert. Use the `handlersInPackage` predicate.
- **`JacksonConfiguration.kt`** — registers a `@Primary` `GeneralConverter` delegating to `Jackson2Converter`.
  - **Do not let Axon fall back to its default Jackson 3 mapper.** Events carry `javax.money.MonetaryAmount`, and Zalando's `MoneyModule` is Jackson 2 only; without this bean every `Money`-bearing event fails to deserialize and the product and order projections silently stop updating.
- **`IdempotencyInterceptor.kt`** — exactly-once processing, keyed `(eventId, processingGroup)`, stored in MongoDB. Derives the processing group by parsing the worker thread name (`WorkPackage[<processor>]-<segment>`), because Axon exposes no processing group on the `ProcessingContext`. `IdempotencyInterceptorTest` pins that naming contract — keep it passing.
- **`DeadLetterProcessor.kt`** — custom `ErrorHandler` persisting failures to `failed_events`. Note `processDeadLettersManually()` only increments a retry counter; it does not replay anything.
- **`ByteaEnforcedPostgresSQLDialect.kt`** — forces BLOB→`bytea`. Still required by the token store.

`src/main/resources/META-INF/orm.xml` maps only `TokenEntry` (BYTEA override). The Axon Server connector ships no `TokenStore`, so the JPA token store on PostgreSQL stays.

### Build constraints

- `jackson-annotations` is pinned to 2.21 in `dependencyManagement`. Axon 5's converter needs `JsonSerializeAs`, which exists only from 2.21; Spring Boot 3.5.0 otherwise pins 2.19.0 and startup fails with `NoClassDefFoundError`.
- `axon.metrics.enabled: false`. `axon-metrics-micrometer` registers `MetricsConfigurationEnhancer` via both `META-INF/services` and Spring auto-configuration; with both active startup fails on `Duplicate key MetricsConfigurationEnhancer`. The ServiceLoader registration still supplies metrics.
- `io.axoniq.framework:axoniq-dead-letter` is declared explicitly. Its parent POM declares it at both compile and test scope, and Gradle honours the test-scoped one, dropping it from the runtime classpath.

### Local infrastructure

`compose.yaml` runs MongoDB, PostgreSQL, Axon Server, and a one-shot `axonserver-init`.

**Do not set `AXONIQ_AXONSERVER_STANDALONE=true`.** It auto-initialises a *non-DCB* `default` context, and the free edition caps you at `_admin` + `default` — leaving no room to create a DCB one. The node starts uninitialised and `axonserver-init` calls `POST /v2/cluster/init?dcb=true`. Axon 5 requires a DCB context.

PostgreSQL is pinned (not `latest`): with a persistent volume, a major-version bump breaks startup with "database files are not compatible".

Dashboard: http://localhost:8024

## Schema evolution

There is **no upcaster**. `ProductCreatedUpcaster` was deleted in `1338313`. Legacy `ProductCreated` events (no SKU) are handled by `sku: String? = null` on the event plus the `?:` fallback in `Product.kt`'s `@EntityCreator`, which assigns `LEGACY-PENDING-{id}`.

## REST API

All endpoints in `RestEndpoint.kt`. Admin UI (Thymeleaf) in `admin/`. Custom actuator endpoint `/actuator/deadletters` exposes DLQ state.

Note: domain invariant violations (`IllegalStateException` from entity command handlers) currently surface as HTTP 500 with the raw exception message, not 400/409.

## Testing

**Command-model tests** use `AxonTestFixture` built from a hand-rolled `EventSourcingConfigurer`:

```kotlin
fixture = AxonTestFixture.with(
    EventSourcingConfigurer.create()
        .registerEntity(EventSourcedEntityModule.autodetected(String::class.java, Product::class.java))
        .componentRegistry { registry ->
            registry.registerComponent(GeneralConverter::class.java) { _ ->
                DelegatingGeneralConverter(Jackson2Converter())
            }
        }
)
```

These build their own configurer rather than reusing production wiring, so they can drift from it.

**Integration tests** run against a real Axon Server container (`TestcontainersConfiguration`), using `TestRestTemplate` + Testcontainers + Awaitility, annotated `@DirtiesContext(AFTER_CLASS)`.

`AxonServerContainer` must be built with `.withDcbContext(true)`. Axon 5 requires a DCB-enabled context and a default-initialised node has none. `@ServiceConnection` supplies the address via `AxonServerTestContainerConnectionDetailsFactory` — no `@DynamicPropertySource` needed.

### Test isolation

**Keep `@DirtiesContext(AFTER_CLASS)` on every integration test class.** It is what makes them independent: disposing the context disposes the container beans, so each class gets a fresh Axon Server, PostgreSQL and MongoDB. Verified — a two-class run creates two of each container.

Remove it and the containers become shared, at which point events persist across classes and assertions turn order-dependent.

Do **not** "fix" that by purging events from Axon Server between classes. Tracking tokens live in PostgreSQL and would survive the purge, leaving processors with tokens pointing past the end of a truncated stream. Either reset both stores or neither.

The cost of this isolation is startup time — roughly 20s per additional class. Prefer adding tests to an existing class over adding a new class.

Assert on observable state, not just non-null. Two bugs in this repo survived for exactly that reason: a DLQ test that asserted nothing about the DLQ, and an idempotency test asserting `processingGroup` was non-null while every record was landing in the `"default"` fallback.
