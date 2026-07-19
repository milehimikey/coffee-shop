# Coffee Shop - Axon Framework Demo Application

A demonstration application showcasing various features of the Axon Framework, including:
- Event Sourcing
- CQRS (Command Query Responsibility Segregation)
- Schema Evolution (nullable fields + defaults at reconstruction)
- Distributed command/query buses over Axon Server
- Dead Letter Queue Processing
- Two read-model technologies from one event stream (MongoDB + PostgreSQL)

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Schema Evolution (Legacy Products Without SKU)](#schema-evolution-legacy-products-without-sku)
- [Features](#features)
- [API Documentation](#api-documentation)
- [Technology Stack](#technology-stack)

## Overview

This application simulates a coffee shop order management system with products, orders, and payments. It demonstrates how to build an event-sourced application using Axon Framework with proper domain-driven design principles.

## Architecture

The application follows a CQRS architecture with event sourcing:

- **Command Side**: Handles commands and produces domain events
- **Query Side**: Maintains read models optimized for queries
- **Event Store**: Axon Server (DCB context), reached over gRPC
- **Command/Query Buses**: Distributed via Axon Server, not in-process
- **Projections**: Update query models based on events - MongoDB for operational views, PostgreSQL for reporting

### Domain Model

Entities are Axon 5 `@EventSourced` classes, not Axon 4 aggregates.

- **Product**: Manages product lifecycle (create, update, delete)
- **Order**: Manages order lifecycle (create, add items, submit, deliver, complete)
- **Payment**: Manages payment processing (create, process, fail, refund)
- **Reporting**: Read-only daily revenue rollup on PostgreSQL, fed by `OrderCompleted` and `PaymentProcessed`

## Prerequisites

- Java 21 or higher
- Docker and Docker Compose (for Axon Server, PostgreSQL and MongoDB)
- Gradle (wrapper included)

## Getting Started

### 1. Start Infrastructure

Start Axon Server, PostgreSQL and MongoDB using Docker Compose:

```bash
docker-compose up -d
```

This will start:
- Axon Server on ports 8024 (dashboard) and 8124 (gRPC)
- PostgreSQL on port 5432
- MongoDB on port 27017

### 2. Build the Application

```bash
./gradlew build
```

### 3. Run the Application

**Standard Mode:**
```bash
./gradlew bootRun
```

**With Legacy Data (products whose events carry no SKU):**
```bash
./gradlew bootRun --args='--spring.profiles.active=legacy-data'
```

The application will start on `http://localhost:8080`

### 4. Access the UI

- **Dashboard**: http://localhost:8080/
- **Data Generator**: http://localhost:8080/generator

## Schema Evolution (Legacy Products Without SKU)

Legacy `ProductCreated` events carry no SKU. They are handled by a nullable field plus a
default at reconstruction time — **not** by an upcaster.

`ProductCreatedUpcaster` was deleted in commit `1338313`. Axon 5 handles schema evolution by
payload conversion at handling time rather than an upcaster chain, and for this case a
nullable field is sufficient.

### How it works

1. `Products.Events.kt` declares `sku: String? = null`, so old events deserialize cleanly.
2. `Product.kt`'s `@EntityCreator` applies the fallback when loading the entity:
   ```kotlin
   sku = event.sku ?: "LEGACY-PENDING-${event.id.take(8)}"
   ```
3. Nothing rewrites stored events. The original event keeps `sku = null` forever; the default
   is applied on every load.

### Trying it

Start with legacy data, which generates products whose events have no SKU:

```bash
./gradlew bootRun --args='--spring.profiles.active=legacy-data'
```

Or generate them on demand:

```bash
curl -X POST http://localhost:8080/api/generate/legacy-products \
  -H "Content-Type: application/json" \
  -d '{"count": 5}'
```

Then query products and look for one with `(Legacy` in the name — its SKU will read
`LEGACY-PENDING-...`, supplied by the `@EntityCreator` default:

```bash
curl http://localhost:8080/api/products
```

The generator UI's **"Demonstrate Upcaster"** button and the
`POST /api/generate/demonstrate-upcaster` endpoint still work, but they demonstrate this
fallback, not upcasting. Both are misnamed.

> `SkuLookupService.kt` (CSV → name-based → ID-based SKU derivation) existed to serve the
> deleted upcaster and is **currently not called by any production code**. It still has test
> coverage. Treat it as dead code pending removal or rewiring.

## Features

### Data Generation

The application includes comprehensive data generation capabilities:

- **Batch Generation**: Generate products, orders, and payments in bulk
- **Legacy Products**: Generate products whose events carry no SKU
- **Dead Letter Triggering**: Generate scenarios that produce dead letters

### Dead Letter Queue

The application demonstrates dead letter queue handling:

- Failed events captured by a custom `ErrorHandler` into `failed_events`
- Inspection via `/actuator/deadletters`
- Note: `processDeadLettersManually()` only increments a retry counter - it does not replay events

### PostgreSQL's Role

Events live in Axon Server, so PostgreSQL is no longer the event store. It now holds:

| Table | Purpose |
|---|---|
| `token_entry` | Tracking tokens - the Axon Server connector ships no `TokenStore` |
| `failed_events` | Custom dead-letter store |
| `daily_revenue` | The JPA reporting projection |

## API Documentation

### Products

- `GET /api/products` - List all products
- `GET /api/products/{id}` - Get product by ID
- `POST /api/products` - Create a new product
- `PUT /api/products/{id}` - Update a product
- `DELETE /api/products/{id}` - Delete a product

### Orders

- `GET /api/orders` - List all orders
- `GET /api/orders/{id}` - Get order by ID
- `POST /api/orders` - Create a new order
- `POST /api/orders/{id}/items` - Add item to order
- `POST /api/orders/{id}/submit` - Submit order
- `POST /api/orders/{id}/complete` - Complete order
- `POST /api/orders/{id}/deliver` - Deliver order

### Payments

- `GET /api/payments` - List all payments
- `GET /api/payments/{id}` - Get payment by ID
- `POST /api/payments` - Create a payment
- `POST /api/payments/{id}/process` - Process payment
- `POST /api/payments/{id}/fail` - Fail payment
- `POST /api/payments/{id}/refund` - Refund payment

### Data Generation

- `POST /api/generate/batch` - Generate batch data
- `POST /api/generate/products` - Generate products
- `POST /api/generate/orders` - Generate orders
- `POST /api/generate/legacy-products` - Generate products whose events carry no SKU
- `POST /api/generate/demonstrate-upcaster` - Misnamed: demonstrates the `@EntityCreator` SKU fallback, not upcasting

### Reporting (PostgreSQL read model)

- `GET /api/reporting/revenue?limit={n}` - Daily revenue rollups, newest first (default 30)
- `GET /api/reporting/revenue/{date}` - Rollup for one ISO date, e.g. `2026-07-18`

## Technology Stack

- **Axon Framework 5.2.0**: Event sourcing and CQRS framework (`io.axoniq.framework`, AxonIQ Terms of Service - not Apache 2.0)
- **Spring Boot 3.x**: Application framework
- **Kotlin**: Programming language
- **Axon Server**: Event store and message routing (requires a DCB-enabled context)
- **MongoDB**: Query model storage
- **Thymeleaf**: Server-side templating
- **Bootstrap 5**: UI framework

## License

This is a demonstration application for educational purposes.

