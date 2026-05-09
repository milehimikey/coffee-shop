package wtf.milehimikey.coffeeshop.products

import org.axonframework.conversion.DelegatingGeneralConverter
import org.axonframework.conversion.GeneralConverter
import org.axonframework.conversion.jackson2.Jackson2Converter
import org.axonframework.eventsourcing.configuration.EventSourcedEntityModule
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer
import org.axonframework.messaging.eventhandling.EventMessage
import org.axonframework.test.fixture.AxonTestFixture
import org.javamoney.moneta.Money
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class ProductCommandTests {

    private lateinit var fixture: AxonTestFixture

    @BeforeEach
    fun setUp() {
        fixture = AxonTestFixture.with(
            EventSourcingConfigurer.create()
                .registerEntity(EventSourcedEntityModule.autodetected(String::class.java, Product::class.java))
                .componentRegistry { registry ->
                    registry.registerComponent(GeneralConverter::class.java) { _ -> DelegatingGeneralConverter(Jackson2Converter()) }
                }
        )
    }

    @AfterEach
    fun tearDown() {
        fixture.stop()
    }

    @Test
    fun `should create product`() {
        val command = CreateProduct(
            id = "product-1",
            name = "Espresso",
            description = "Strong coffee",
            price = Money.of(BigDecimal("3.50"), "USD"),
            sku = "ESP-001"
        )
        val expectedEvent = ProductCreated(
            id = "product-1",
            name = "Espresso",
            description = "Strong coffee",
            price = Money.of(BigDecimal("3.50"), "USD"),
            sku = "ESP-001"
        )

        fixture.given().noPriorActivity()
            .`when`().command(command)
            .then().success().events(expectedEvent)
    }

    @Test
    fun `should update product`() {
        val id = "product-1"
        val updateCommand = UpdateProduct(
            id = id,
            name = "Double Espresso",
            description = "Extra strong coffee",
            price = Money.of(BigDecimal("4.50"), "USD")
        )
        val expectedEvent = ProductUpdated(
            id = id,
            name = "Double Espresso",
            description = "Extra strong coffee",
            price = Money.of(BigDecimal("4.50"), "USD")
        )

        fixture.given().events(
            ProductCreated(
                id = id,
                name = "Espresso",
                description = "Strong coffee",
                price = Money.of(BigDecimal("3.50"), "USD"),
                sku = "ESP-001"
            )
        )
            .`when`().command(updateCommand)
            .then().success().events(expectedEvent)
    }

    @Test
    fun `should delete product`() {
        val id = "product-1"
        val name = "Espresso"
        val description = "Strong coffee"
        val price = Money.of(BigDecimal("3.50"), "USD")

        fixture.given().events(
            ProductCreated(id = id, name = name, description = description, price = price, sku = "ESP-001")
        )
            .`when`().command(DeleteProduct(id = id))
            .then().success().eventsSatisfy { events: List<EventMessage> ->
                check(events.size == 1) { "Expected 1 event, got ${events.size}" }
                val event = events[0].payload() as ProductDeleted
                check(event.id == id)
                check(event.name == name)
                check(event.description == description)
                check(event.price == price)
            }
    }

    @Test
    fun `should not update deleted product`() {
        val id = "product-1"

        fixture.given().events(
            ProductCreated(
                id = id,
                name = "Espresso",
                description = "Strong coffee",
                price = Money.of(BigDecimal("3.50"), "USD"),
                sku = "ESP-001"
            ),
            ProductDeleted(
                id = id,
                name = "Espresso",
                description = "Strong coffee",
                price = Money.of(BigDecimal("3.50"), "USD"),
                deletedAt = Instant.now()
            )
        )
            .`when`().command(
                UpdateProduct(
                    id = id,
                    name = "Double Espresso",
                    description = "Extra strong coffee",
                    price = Money.of(BigDecimal("4.50"), "USD")
                )
            )
            .then().exception(IllegalStateException::class.java)
    }

    @Test
    fun `should not delete already deleted product`() {
        val id = "product-1"

        fixture.given().events(
            ProductCreated(
                id = id,
                name = "Espresso",
                description = "Strong coffee",
                price = Money.of(BigDecimal("3.50"), "USD"),
                sku = "ESP-001"
            ),
            ProductDeleted(
                id = id,
                name = "Espresso",
                description = "Strong coffee",
                price = Money.of(BigDecimal("3.50"), "USD"),
                deletedAt = Instant.now()
            )
        )
            .`when`().command(DeleteProduct(id = id))
            .then().exception(IllegalStateException::class.java)
    }
}
