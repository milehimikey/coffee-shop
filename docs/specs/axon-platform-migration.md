# Spec: Migration to Axon Platform (Axon 5 + Axon Server)

**Status:** Largely implemented. See the corrections below — the original blocking constraint was false.
**Date:** 2026-07-18
**Scope:** Move event storage and message routing onto Axon Server; upgrade Axon Framework 5.1.0 → 5.2.0; repurpose PostgreSQL from event store to token store + DLQ + JPA read model.

## Overview

The goal is to convert this application to Axon 5 + Axon Server. Half of that is already done: commits `1338313` / `77abe9f` migrated the codebase to **Axon Framework 5.1.0** — `@EventSourced(tagKey=…)`, `@EntityCreator`, `EventAppender`, `@TargetEntityId`, `@EventTag`, `AxonTestFixture`. No Axon 4 API remains anywhere in `src/`.

What is missing is the **platform** side. The application currently runs Axon standalone:

- `axon.axonserver.enabled: false`
- Event store = PostgreSQL via Axon's JPA storage engine
- Command and query buses = local, in-process

This spec covers closing that gap.

### Framework upgrade: 5.1.0 → 5.2.0

`5.2.0` is the current release on Maven Central (the line runs `5.1.0, 5.1.1, 5.1.2, 5.2.0-RC1, 5.2.0`). The upgrade is low-risk for this codebase: every Axon API it touches is unchanged in 5.2.0 — `EventProcessorDefinition`, `EventProcessorDefinition$ConfigurationStep`, `EventHandlerSelector`, and the `@EventSourced` stereotype are all still present in `axon-spring-5.2.0.jar`, and the `axon.converter.*` / `axon.eventhandling.*` / `axon.eventstorage.jpa.*` property sets are identical.

Do the version bump as its own commit and confirm the suite is green **before** layering Axon Server on top, so the two changes stay separable and independently revertable.

## RESOLVED: the connector moved group id, it is not paywalled

> **Correction, applied during implementation.** The section that stood here declared Axon
> Server unobtainable without commercial credentials. That conclusion was wrong, and acting on
> it would have blocked the migration indefinitely.

`org.axonframework:axon-server-connector` does stop at `5.1.0-RC2` — that observation was
correct. But AxonIQ **changed the group id**. Verified against live Maven Central metadata:

| Coordinate | Status |
|---|---|
| `org.axonframework:axon-server-connector` | stops at `5.1.0-RC2` (the original evidence) |
| `io.axoniq.framework:axon-server-connector` | **`5.2.0`, public on Maven Central** |
| `io.axoniq.framework:axoniq-spring-boot-starter` | `5.2.0` — ships `AxonServerAutoConfiguration` |
| `io.axoniq.framework:axoniq-testcontainer` | `5.2.0` — ships `AxonServerContainer` and `AxonServerContainerUtils` |

No commercial Maven repository and no build credentials are required.

**Do not add the `repo.axoniq.io` repository suggested in §1.** Both `repo.axoniq.io` and
`nexus.axoniq.io` return HTTP 200 with an HTML marketing SPA for *every* path, including
invented ones. Gradle would receive HTML where it expects XML and fail with a confusing parse
error rather than a clean 401.

### The real constraint is licensing, not availability

These artifacts are published under the **AxonIQ Terms of Service, not Apache 2.0**, and
enforce licensing at runtime:

```
[AXONIQ] No license is available for this application.
The JVM will shut down after a 15-minute grace period if no valid license is found.
```

Without a license the application runs in 15-minute windows. Test suites finish well inside
that, so CI is unaffected. The Sequenced Dead-Letter Queue (`framework.dead_letter_queue`) is a
separately licensed addon that is detected but never enrols events without a license.

The 5.0.5 fallback below remains valid if Apache 2.0 licensing is required.

### Also note

`io.axoniq.platform:axoniq-platform-spring-boot-starter` is **not** Axon Server. It is AxonIQ
Console, a monitoring agent (`io.axoniq.console.framework.starter`, connects to
`framework.console.axoniq.io:7000`). Its `AxoniqPlatformEventStorageEngine` wraps a delegate
`EventStorageEngine` to collect metrics — it is not an event store.

---|---|---|---|---|
| `axon-server-connector` on Maven Central | yes | yes | **absent** | **absent** |
| `axon-server-connector` in `axon-framework-bom` | yes | yes | **absent** | **absent** |
| `AxonServerAutoConfiguration` in Spring starter | yes | yes | **absent** | **absent** |
| `AxonServerActuatorAutoConfiguration` | yes | yes | **absent** | **absent** |
| `AxonServerContainer` in `axon-test` | — | yes | **absent** | **absent** |

Upgrading to 5.2.0 does not dissolve this constraint — the commercial split is deliberate and persistent, not a 5.1.0 packaging accident. `nexus.axoniq.io` now serves a marketing SPA on every path, so there is no publicly reachable Maven repo for the connector at any 5.1+ version.

**This spec therefore targets the commercial Axon Platform distribution at 5.2.0**, with the repository URL and credentials supplied externally.

### Confidence note

Everything below except the exact commercial coordinates is verified against real published jars: `5.2.0` for the framework and Spring extension, and `5.1.0-RC2` for the connector itself, which is the newest connector build that is publicly inspectable. Connector API details should be treated as high-confidence but **not** 5.2.0-verified.

### Alternative considered

Pinning the whole stack to **5.0.5**, where `axon-server-connector` and `AxonServerAutoConfiguration` are on Maven Central under Apache 2.0, would make this migration fully open source and buildable today. It was rejected in favour of staying on the current release line, but it remains the fallback if licensing is delayed or declined.

---

## 1. Build — `build.gradle.kts`

Bump the framework version:

```kotlin
extra["axon.version"] = "5.2.0"   // was 5.1.0
```

`axon-framework-bom:5.2.0` still manages `axon-spring-boot-starter`, `axon-metrics-micrometer`, and `axon-test`, so those stay versionless.

Add the AxonIQ commercial repository. **No secrets belong in this repository** — resolve them from `~/.gradle/gradle.properties` or the environment:

```kotlin
repositories {
    mavenCentral()
    maven {
        // TODO: replace with the repository URL from the AxonIQ Platform account
        url = uri(providers.gradleProperty("axoniqRepoUrl")
            .orElse(providers.environmentVariable("AXONIQ_REPO_URL"))
            .getOrElse("https://repo.axoniq.io/repository/axon-releases/"))
        credentials {
            username = providers.gradleProperty("axoniqUsername")
                .orElse(providers.environmentVariable("AXONIQ_USERNAME")).orNull
            password = providers.gradleProperty("axoniqPassword")
                .orElse(providers.environmentVariable("AXONIQ_PASSWORD")).orNull
        }
    }
}
```

Add the connector. The OSS `axon-framework-bom:5.2.0` does **not** manage this coordinate — it lists only `axon-common`, `axon-conversion`, `axon-eventsourcing`, `axon-messaging`, `axon-modelling`, `axon-test`, `axon-update`, the Kotlin and metrics extensions, and the Spring extension — so pin the version explicitly:

```kotlin
implementation("org.axonframework:axon-server-connector:${property("axon.version")}")
```

Verify at implementation time whether the commercial distribution ships a different `axon-test` containing `org.axonframework.test.server.AxonServerContainer`. If it does, use it; if not, §7 has a `GenericContainer` fallback.

Keep `spring-boot-starter-data-jpa`, `postgresql`, and the Mongo starter — all three still have a job (§4, §5).

---

## 2. Configuration — `src/main/resources/application.yml`

Enable Axon Server:

```yaml
axon:
  axonserver:
    enabled: true
    servers: localhost:8124
    context: default
    component-name: coffee-shop
```

Valid `axon.axonserver.*` keys are confirmed from the connector's own `spring-configuration-metadata.json` (`servers` default `localhost`, `context` default `default`, `event-store.enabled` default `true`, plus flow-control, heartbeat, and SSL groups).

### Two latent config bugs to fix while here

Both were found during research; both are currently silent no-ops.

1. **`axon.serializer.general|events|messages: jackson` is not a valid Axon 5 property.** The 5.2.0 autoconfigure metadata defines `axon.converter.general|events|messages` (type `ConverterProperties$ConverterType`, default `default`). Rename these three keys, or delete them — Jackson is already the default.
2. **Mongo is configured under `spring.mongodb`**, with a comment claiming Spring Boot 4.0 moved it there. This project is on **Boot 3.5.0**, which binds `spring.data.mongodb`. Confirm whether local runs are silently falling back to an unauthenticated default connection; if so, move the block back to `spring.data.mongodb`.

The `axon.eventhandling.processors.*` block is now largely redundant — `AxonConfig.kt` already declares all three processors as `pooledStreaming` via `EventProcessorDefinition` beans. Keep `batch-size` / `thread-count` there if they should stay property-driven; drop `mode`.

---

## 3. Event store migration to Axon Server

The connector's `AxonServerEventStorageEngine` is **DCB-native** — it talks to Axon Server over `DcbEventChannel` and works in terms of `AppendCondition` / `SourcingCondition` / `TaggedEventMessage`. The domain model already speaks this dialect: `@EventSourced(tagKey = "orderId")` on `orders/Order.kt`, `"paymentId"` on `payments/Payment.kt`, `"id"` on `products/Product.kt`, with matching `@EventTag(key = …)` on every event.

**No domain code changes are required.** This is the single biggest reason the migration is tractable.

### Changes

- **`src/main/resources/META-INF/orm.xml`** — delete the `AggregateEventEntry` entity block (the payload/metadata BYTEA overrides). **Keep** the `TokenEntry` block; see §4.
- **`config/ByteaEnforcedPostgresSQLDialect.kt`** — keep. Still needed for the token store's BYTEA column.
- Requires an Axon Server version with DCB support. `axoniq/axonserver:2026.0.4` is current.

### Data migration

None required. `spring.jpa.hibernate.ddl-auto: create` means the existing Postgres event store is already wiped on every boot, so there is no production history to carry over. Anyone with a long-lived local database should know their existing event data is discarded.

### Behavioral change worth flagging

With the connector present, the command and query buses become **distributed** (`DistributedCommandBusConnector`, `DistributedQueryBusConfiguration`, and the `PayloadConverting*Connector` decorators). Commands and queries now round-trip through Axon Server over gRPC rather than dispatching in-process.

Expect added latency and new failure modes — `AxonServerCommandDispatchException`, `AxonServerRemoteQueryHandlingException` — surfacing in `RestEndpoint.kt`'s `.exceptionally { … }` branches.

---

## 4. PostgreSQL's new role

Postgres stays, with three jobs:

1. **Token store (required).** The connector jar contains **no `TokenStore` implementation** — verified by listing every class in `axon-server-connector-5.1.0-RC2.jar`. Axon Server does not hold streaming-processor tokens for pooled-streaming processors, so the JPA token store on Postgres remains, and so does its `orm.xml` BYTEA override.
2. **Custom DLQ (kept as-is).** `config/DeadLetterProcessor.kt`, `config/FailedEventRecord.kt`, `config/DeadLetterView.kt`, and the `/actuator/deadletters` endpoint carry forward unchanged. The `ErrorHandler` stays wired through `EventProcessorDefinition.customized { it.errorHandler(...) }` in `AxonConfig.kt`.
3. **A JPA read model** — new, see §5.

`config/IdempotencyInterceptor.kt` and its Mongo `idempotency_records` collection also carry forward unchanged.

> **Pre-existing weaknesses. Status after implementation:**
> - `DeadLetterProcessor.processDeadLettersManually()` only increments a retry counter — it never replays anything. **Still true; still out of scope.**
> - `IdempotencyInterceptor` regex-scraping the thread name — **was already broken, now fixed.** The pattern matched `Processor[...]`, but pooled processors emit `WorkPackage[<processor>]-<segment>`, so every event fell into the `"default"` group and all processors shared one idempotency namespace. Since the key is `(eventId, processingGroup)`, one projection could suppress an event another still needed. `IdempotencyInterceptorTest` now pins the naming contract.
>
> **A third, larger defect found during implementation:** all four `EventProcessorDefinition` beans were inert. `EventHandlerSelector.matchesNamespaceOnType` matches a `namespace` *attribute* on an annotation on the handler type — not the Java package its argument looks like. Nothing declared one, so no selector matched, handlers fell into per-package default processors, the custom `ErrorHandler` was never installed (hence no DLQ capture), and every `axon.eventhandling.processors.*` setting applied to processors that did not exist. Fixed by selecting on `beanType().packageName`.

---

## 5. New JPA projection on Postgres

The point of this addition is to demonstrate two read-model technologies fed from one event stream: Mongo for the existing per-context views, Postgres for reporting.

New package `wtf.milehimikey.coffeeshop.reporting/`, mirroring the existing per-context file layout so it reads like the rest of the codebase:

| File | Contents |
|---|---|
| `ReportingProjection.kt` | `@Component` with `@EventHandler` methods on `OrderCompleted` + `PaymentProcessed`, writing to JPA |
| `Repositories.kt` | `@Entity` revenue/summary row + `JpaRepository` |
| `Queries.kt` | Query data classes + `@QueryHandler` methods, `@Transactional(readOnly = true)` — matching the shape in `products/Queries.kt` |

Wire a fourth processor in `config/AxonConfig.kt`, following the existing three exactly:

```kotlin
@Bean
fun reportingProcessorDefinition(): EventProcessorDefinition =
    EventProcessorDefinition.pooledStreaming("reporting")
        .assigningHandlers(EventHandlerSelector.matchesNamespaceOnType("wtf.milehimikey.coffeeshop.reporting"))
        .customized { config -> config.errorHandler(deadLetterProcessor.errorHandler()) }
```

`build.gradle.kts` already has `allOpen { annotation("jakarta.persistence.Entity") }`, so the new entity needs no extra build configuration. Expose the new queries via `RestEndpoint.kt` using the existing `queryGateway.query(...)` / `queryMany(...)` pattern.

---

## 6. Local infrastructure — `compose.yaml`

Add an Axon Server service:

```yaml
  axonserver:
    image: 'axoniq/axonserver:2026.0.4'
    environment:
      # NOTE: AXONIQ_AXONSERVER_STANDALONE=true was in the original draft and is WRONG.
      # It auto-initialises a non-DCB 'default' context, and the free edition caps you at
      # _admin + default, so there is then no room to create a DCB one. Start uninitialised
      # and POST /v2/cluster/init?dcb=true instead - see compose.yaml's axonserver-init.
      - 'AXONIQ_AXONSERVER_DEVMODE_ENABLED=true'
    ports:
      - '8024:8024'   # dashboard / REST
      - '8124:8124'   # gRPC
    volumes:
      - 'axonserver_data:/axonserver/data'
      - 'axonserver_events:/axonserver/events'
```

No extra wiring is needed: `AxonServerDockerComposeConnectionDetailsFactory` matches on the image name `axoniq/axonserver` (confirmed in the class constant pool), so Spring Boot's Docker Compose integration supplies connection details automatically.

Add `axonserver_data` and `axonserver_events` to the top-level `volumes:` block. The dashboard is at http://localhost:8024.

Also uncomment and extend the `coffee-shop` service's `depends_on` and environment if that service is used.

---

## 7. Tests

**Unaffected:** `ProductCommandTests`, `PaymentCommandTests`, and `OrderCommandTests` build `AxonTestFixture` from a hand-rolled `EventSourcingConfigurer` and never touch Axon Server.

### `TestcontainersConfiguration.kt`

Add an Axon Server container alongside the existing Mongo and Postgres `@ServiceConnection` beans:

- **If** the commercial `axon-test` ships `org.axonframework.test.server.AxonServerContainer`, use it with `@ServiceConnection` — `AxonServerTestContainerConnectionDetailsFactory` is typed exactly against that class and wires it automatically.
- **Otherwise**, fall back to `GenericContainer(DockerImageName.parse("axoniq/axonserver:2026.0.4"))` exposing 8024/8124, plus a `@DynamicPropertySource` setting `axon.axonserver.servers`.

### `src/test/resources/application.yml`

Set `axon.axonserver.enabled: true`. Also delete the `dlq.enabled: true` entries under each processor. **Partially wrong:** that key is dead config on plain OSS Axon 5, but once `io.axoniq.framework:axoniq-dead-letter` is on the classpath a real `DeadLetterQueueProcessorProperties$Dlq` binds it. It is left disabled here because the platform DLQ is a licensed addon that does not capture events without a license.

### New isolation problem to solve

`CoffeeShopApplicationTests` is `@DirtiesContext(AFTER_CLASS)` and relies on `ddl-auto: create` giving it a clean event store. Once events live in Axon Server, a container shared across the suite retains events between classes, and the projection and idempotency assertions become order-dependent. The existing `should ensure idempotent event processing` test already calls `idempotencyRepository.deleteAll()` on a shared context, which compounds the problem.

Pick one:
- Reset the Axon Server context between classes (`AxonServerContainerUtils` has purge support if the commercial `axon-test` provides it; otherwise hit the server's REST API on 8024), or
- Give the class its own container instance.

### Coverage gaps to close

- The current DLQ test asserts nothing about the DLQ, and its comment references a `PaymentEventProcessorTests` class that does not exist.
- Add at minimum a test for the new `reporting` JPA projection.

---

## 8. Documentation cleanup

- **`CLAUDE.md`** — says "Axon Framework 4.12", describes `@Aggregate` / `@TargetAggregateIdentifier` / `AggregateTestFixture`, and documents a `ProductCreatedUpcaster` that was deleted in `1338313`. Rewrite for Axon 5 + Axon Server, and document PostgreSQL's new role.
- **`README.md`** (lines ~89, 167–178) — same stale upcaster references. Schema evolution is now handled by `sku: String? = null` on the event plus the `?:` fallback in `Product.kt`'s `@EntityCreator`.
- **`admin/DataGenerator.kt`** — log lines credited the deleted upcaster; corrected. The "Demonstrate Upcaster" button in `templates/generator.html` still works, but for a different reason than it claims, and is now labelled as such. `products/SkuLookupService.kt` and its test were deleted outright - the service had no production callers once the upcaster was gone.

---

## Verification

1. **Build resolves** — `./gradlew build` succeeds with the commercial repo credentials in place. This is the first real gate: a wrong connector coordinate fails here.
2. **Server connects** — `./gradlew bootRun`, then confirm `coffee-shop` appears as a connected application in the Axon Server dashboard at http://localhost:8024, and that `/actuator/health` reports the `axonServer` indicator up (from `AxonServerActuatorAutoConfiguration`).
3. **Events land in Axon Server** — drive a full order through the REST API (`POST /api/products`, `POST /api/orders`, add item, submit, deliver, complete), then confirm the events appear in the Axon Server dashboard's event browser and that **no** `domain_event_entry` table is created in Postgres.
4. **Postgres holds only its new responsibilities** — inspect the schema: expect `token_entry`, `failed_events`, and the new reporting table; expect no event-store tables.
5. **Both read models update** — the Mongo-backed order/product/payment views and the new Postgres reporting view both reflect the run from step 3.
6. **Distributed buses work end to end** — `admin/DataGenerator.kt` uses blocking `commandGateway.sendAndWait(...)` across ~30 call sites. Run it via the generator UI to shake out gRPC round-trip and timeout behavior the async REST paths will not reveal.
7. **DLQ still functions** — create a product priced at exactly `99.99` (the deliberate trip-wire in `products/EventProcessors.kt`) and confirm a `FailedEventRecord` row appears and `/actuator/deadletters` reports it.
8. **Tests pass** — `./gradlew test`, with attention to `CoffeeShopApplicationTests` for the cross-class Axon Server state leakage described in §7.

---

## Open item

The exact commercial coordinates — repository URL, artifact group and name, whether the connector is published at `5.2.0` at all, and whether a platform-specific `axon-test` exists — need confirmation from the AxonIQ Platform account.

This is worth resolving early, because it carries a sequencing decision. If the commercial connector lags the OSS framework (shipping only at `5.1.x`, say), the choice is between pinning the framework back to match the connector or running a mixed pair. **Confirm the available connector versions before doing the 5.2.0 bump in §1**, so the bump does not have to be walked back.
