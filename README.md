# Coffee Shop - Axon Framework Demo Application

A demonstration application showcasing various features of the Axon Framework, including:
- Event Sourcing
- CQRS (Command Query Responsibility Segregation)
- Event Upcasting (Schema Evolution)
- Saga Pattern
- Dead Letter Queue Processing
- Aggregate Snapshots

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Demonstrating Event Upcasting](#demonstrating-event-upcasting)
- [Features](#features)
- [API Documentation](#api-documentation)
- [Technology Stack](#technology-stack)

## Overview

This application simulates a coffee shop order management system with products, orders, and payments. It demonstrates how to build an event-sourced application using Axon Framework with proper domain-driven design principles.

## Architecture

The application follows a CQRS architecture with event sourcing:

- **Command Side**: Handles commands and produces domain events
- **Query Side**: Maintains read models optimized for queries
- **Event Store**: Stores all domain events (using Postgres)
- **Projections**: Update query models based on events
- **Sagas**: Coordinate complex business processes across aggregates

### Domain Model

- **Product Aggregate**: Manages product lifecycle (create, update, delete)
- **Order Aggregate**: Manages order lifecycle (create, add items, submit, complete, deliver)
- **Payment Aggregate**: Manages payment processing (create, process, fail, refund)

## Prerequisites

- Java 21 or higher
- Docker and Docker Compose (for Postgres and MongoDB)
- Gradle (wrapper included)

## Getting Started

### 1. Start Infrastructure

Start Axon Server and MongoDB using Docker Compose:

```bash
docker-compose up -d
```

This will start:
- Postgres on port 5432
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

**With Legacy Data (for Upcaster Demo):**
```bash
./gradlew bootRun --args='--spring.profiles.active=legacy-data'
```

The application will start on `http://localhost:8080`

### 4. Access the UI

- **Dashboard**: http://localhost:8080/
- **Data Generator**: http://localhost:8080/generator

## Demonstrating Event Upcasting

Event upcasting is a powerful feature for handling schema evolution in event-sourced systems. This application demonstrates upcasting with the `ProductCreatedUpcaster`, which adds SKU fields to old `ProductCreated` events.

### Background

In the early version of this application, products didn't have SKU fields. Later, we added SKU as a required field. The upcaster allows old events (without SKU) to be automatically upgraded when they're replayed from the event store.

### Step-by-Step Demo

#### Option 1: Automatic Demo (Recommended)

1. **Start the application with the legacy-data profile:**
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=legacy-data'
   ```

2. **The application will automatically:**
   - Generate 10 legacy products (without SKU)
   - Display instructions in the console

3. **Demonstrate the upcaster using the UI:**
   - Navigate to http://localhost:8080/generator
   - Click the **"Demonstrate Upcaster"** button
   - Check the application logs for upcaster activity

4. **What to look for in the logs:**
   ```
   Upcasting ProductCreated event for product <id>: adding SKU = <sku>
   ```

#### Option 2: Manual Demo via REST API

1. **Generate legacy products:**
   ```bash
   curl -X POST http://localhost:8080/api/generate/legacy-products \
     -H "Content-Type: application/json" \
     -d '{"count": 5}'
   ```

2. **Demonstrate the upcaster:**
   ```bash
   curl -X POST http://localhost:8080/api/generate/demonstrate-upcaster \
     -H "Content-Type: application/json" \
     -d '{}'
   ```

3. **Check the logs** for upcaster activity

#### Option 3: Manual Demo via Product API

1. **Start with legacy-data profile** (as shown above)

2. **Query all products:**
   ```bash
   curl http://localhost:8080/api/products
   ```

3. **Find a product with "(Legacy" in the name** and note:
   - It HAS a SKU field (added by the upcaster!)
   - The SKU was NOT in the original event

4. **Update the product to trigger event replay:**
   ```bash
   curl -X PUT http://localhost:8080/api/products/{product-id} \
     -H "Content-Type: application/json" \
     -d '{
       "name": "Updated Product Name",
       "description": "Updated description",
       "price": 5.99
     }'
   ```

5. **Watch the logs** - you'll see the upcaster adding the SKU when the aggregate is loaded

### How It Works

1. **Legacy Event Creation**: When you generate legacy products, the application creates `ProductCreated` events with `sku = null`
2. **Event Storage**: These events are stored in the Axon Server event store without SKU
3. **Event Replay**: When an aggregate is loaded (e.g., to handle an update command), all events are replayed
4. **Upcasting**: The `ProductCreatedUpcaster` intercepts old events and adds the SKU field using the `SkuLookupService`
5. **Aggregate Reconstruction**: The aggregate receives the upcasted event with the SKU field

### Upcaster Implementation

The upcaster uses a multi-strategy approach to generate SKUs:

1. **CSV Lookup**: Checks `sku-mappings.csv` for predefined mappings
2. **Name-based**: Generates SKU from product name (e.g., "Espresso" → "ESP-001")
3. **ID-based**: Falls back to using product ID (e.g., "SKU-{id}")

See `ProductCreatedUpcaster.kt` and `SkuLookupService.kt` for implementation details.

## Features

### Data Generation

The application includes comprehensive data generation capabilities:

- **Batch Generation**: Generate products, orders, and payments in bulk
- **Legacy Products**: Generate products without SKU for upcaster demo
- **Snapshot Triggering**: Generate enough events to trigger aggregate snapshots
- **Dead Letter Triggering**: Generate scenarios that produce dead letters

### Dead Letter Queue

The application demonstrates dead letter queue handling:

- Automatic retry with exponential backoff
- Manual dead letter processing
- Dead letter inspection and diagnostics

### Aggregate Snapshots

Aggregates are configured to create snapshots after a certain number of events:

- Products: Every 10 events
- Orders: Every 20 events
- Payments: Every 15 events

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
- `POST /api/generate/legacy-products` - Generate legacy products (for upcaster demo)
- `POST /api/generate/demonstrate-upcaster` - Demonstrate upcaster functionality

## Technology Stack

- **Axon Framework 4.x**: Event sourcing and CQRS framework
- **Spring Boot 3.x**: Application framework
- **Kotlin**: Programming language
- **Axon Server**: Event store and message routing
- **MongoDB**: Query model storage
- **Thymeleaf**: Server-side templating
- **Bootstrap 5**: UI framework

## License

This is a demonstration application for educational purposes.

