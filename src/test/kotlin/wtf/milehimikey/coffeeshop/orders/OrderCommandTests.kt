package wtf.milehimikey.coffeeshop.orders

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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant

class OrderCommandTests {

    private lateinit var fixture: AxonTestFixture
    private val orderTotalCalculator = mock(OrderTotalCalculator::class.java)

    @BeforeEach
    fun setUp() {
        fixture = AxonTestFixture.with(
            EventSourcingConfigurer.create()
                .registerEntity(EventSourcedEntityModule.autodetected(String::class.java, Order::class.java))
                .componentRegistry { registry ->
                    registry.registerComponent(GeneralConverter::class.java) { _ -> DelegatingGeneralConverter(Jackson2Converter()) }
                    registry.registerComponent(OrderTotalCalculator::class.java) { _ -> orderTotalCalculator }
                }
        )
    }

    @AfterEach
    fun tearDown() {
        fixture.stop()
    }

    @Test
    fun `should create order`() {
        val command = CreateOrder(id = "order-1", customerId = "customer-1")
        val expectedEvent = OrderCreated(id = "order-1", customerId = "customer-1")

        fixture.given().noPriorActivity()
            .`when`().command(command)
            .then().success().events(expectedEvent)
    }

    @Test
    fun `should add item to order`() {
        val orderId = "order-1"
        val customerId = "customer-1"
        val addItemCommand = AddItemToOrder(
            orderId = orderId,
            productId = "product-1",
            productName = "Espresso",
            quantity = 2,
            price = Money.of(BigDecimal("3.50"), "USD")
        )
        val expectedEvent = ItemAddedToOrder(
            orderId = orderId,
            productId = "product-1",
            productName = "Espresso",
            quantity = 2,
            price = Money.of(BigDecimal("3.50"), "USD")
        )

        fixture.given().events(OrderCreated(id = orderId, customerId = customerId))
            .`when`().command(addItemCommand)
            .then().success().events(expectedEvent)
    }

    @Test
    fun `should submit order`() {
        val orderId = "order-1"
        val customerId = "customer-1"
        `when`(orderTotalCalculator.calculateTotal(anyList())).thenReturn(Money.of(BigDecimal("7.00"), "USD"))

        fixture.given().events(
            OrderCreated(id = orderId, customerId = customerId),
            ItemAddedToOrder(
                orderId = orderId,
                productId = "product-1",
                productName = "Espresso",
                quantity = 2,
                price = Money.of(BigDecimal("3.50"), "USD")
            )
        )
            .`when`().command(SubmitOrder(orderId = orderId))
            .then().success().events(OrderSubmitted(orderId = orderId, totalAmount = Money.of(BigDecimal("7.00"), "USD")))
    }

    @Test
    fun `should not submit empty order`() {
        val orderId = "order-1"
        val customerId = "customer-1"

        fixture.given().events(OrderCreated(id = orderId, customerId = customerId))
            .`when`().command(SubmitOrder(orderId = orderId))
            .then().exception(IllegalStateException::class.java)
    }

    @Test
    fun `should deliver order`() {
        val orderId = "order-1"
        val customerId = "customer-1"

        fixture.given().events(
            OrderCreated(id = orderId, customerId = customerId),
            ItemAddedToOrder(
                orderId = orderId,
                productId = "product-1",
                productName = "Espresso",
                quantity = 2,
                price = Money.of(BigDecimal("3.50"), "USD")
            ),
            OrderSubmitted(orderId = orderId, totalAmount = Money.of(BigDecimal("7.00"), "USD"))
        )
            .`when`().command(DeliverOrder(orderId = orderId))
            .then().success().eventsSatisfy { events: List<EventMessage> ->
                check(events.size == 1) { "Expected 1 event, got ${events.size}" }
                val event = events[0].payload() as OrderDelivered
                check(event.orderId == orderId)
                check(event.customerId == customerId)
                check(event.items.size == 1)
                check(event.items[0].productId == "product-1")
                check(event.items[0].productName == "Espresso")
                check(event.items[0].quantity == 2)
                check(event.totalAmount == Money.of(BigDecimal("7.00"), "USD"))
            }
    }

    @Test
    fun `should not deliver unsubmitted order`() {
        val orderId = "order-1"
        val customerId = "customer-1"

        fixture.given().events(
            OrderCreated(id = orderId, customerId = customerId),
            ItemAddedToOrder(
                orderId = orderId,
                productId = "product-1",
                productName = "Espresso",
                quantity = 2,
                price = Money.of(BigDecimal("3.50"), "USD")
            )
        )
            .`when`().command(DeliverOrder(orderId = orderId))
            .then().exception(IllegalStateException::class.java)
    }

    @Test
    fun `should complete order`() {
        val orderId = "order-1"
        val customerId = "customer-1"

        fixture.given().events(
            OrderCreated(id = orderId, customerId = customerId),
            ItemAddedToOrder(
                orderId = orderId,
                productId = "product-1",
                productName = "Espresso",
                quantity = 2,
                price = Money.of(BigDecimal("3.50"), "USD")
            ),
            OrderSubmitted(orderId = orderId, totalAmount = Money.of(BigDecimal("7.00"), "USD")),
            OrderDelivered(
                orderId = orderId,
                customerId = customerId,
                items = listOf(
                    OrderItemData(
                        productId = "product-1",
                        productName = "Espresso",
                        quantity = 2,
                        price = Money.of(BigDecimal("3.50"), "USD")
                    )
                ),
                totalAmount = Money.of(BigDecimal("7.00"), "USD"),
                deliveredAt = Instant.now()
            )
        )
            .`when`().command(CompleteOrder(orderId = orderId))
            .then().success().eventsSatisfy { events: List<EventMessage> ->
                check(events.size == 1) { "Expected 1 event, got ${events.size}" }
                val event = events[0].payload() as OrderCompleted
                check(event.orderId == orderId)
                check(event.customerId == customerId)
                check(event.items.size == 1)
                check(event.items[0].productId == "product-1")
                check(event.items[0].productName == "Espresso")
                check(event.items[0].quantity == 2)
                check(event.totalAmount == Money.of(BigDecimal("7.00"), "USD"))
            }
    }

    @Test
    fun `should not complete undelivered order`() {
        val orderId = "order-1"
        val customerId = "customer-1"

        fixture.given().events(
            OrderCreated(id = orderId, customerId = customerId),
            ItemAddedToOrder(
                orderId = orderId,
                productId = "product-1",
                productName = "Espresso",
                quantity = 2,
                price = Money.of(BigDecimal("3.50"), "USD")
            ),
            OrderSubmitted(orderId = orderId, totalAmount = Money.of(BigDecimal("7.00"), "USD"))
        )
            .`when`().command(CompleteOrder(orderId = orderId))
            .then().exception(IllegalStateException::class.java)
    }

    @Test
    @Disabled("This test is for demonstration purposes only and may take a long time to run")
    fun `should handle many events which would benefit from snapshotting`() {
        val orderId = "order-1"
        val customerId = "customer-1"
        `when`(orderTotalCalculator.calculateTotal(anyList())).thenReturn(Money.of(BigDecimal("60.00"), "USD"))

        val events = mutableListOf<Any>(OrderCreated(id = orderId, customerId = customerId))
        for (i in 1..60) {
            events.add(
                ItemAddedToOrder(
                    orderId = orderId,
                    productId = "product-$i",
                    productName = "Product $i",
                    quantity = 1,
                    price = Money.of(BigDecimal("1.00"), "USD")
                )
            )
        }

        fixture.given().events(events)
            .`when`().command(SubmitOrder(orderId = orderId))
            .then().success().events(
                OrderSubmitted(orderId = orderId, totalAmount = Money.of(BigDecimal("60.00"), "USD"))
            )
    }

    @Test
    fun `should correct product name with compensating event`() {
        val orderId = "order-1"
        val customerId = "customer-1"
        val productId = "product-1"

        fixture.given().events(
            OrderCreated(id = orderId, customerId = customerId),
            ItemAddedToOrder(
                orderId = orderId,
                productId = productId,
                productName = "Bad Name",
                quantity = 2,
                price = Money.of(BigDecimal("3.50"), "USD")
            )
        )
            .`when`().command(
                CorrectOrderItemProductName(
                    orderId = orderId,
                    productId = productId,
                    correctedProductName = "Espresso"
                )
            )
            .then().success().eventsSatisfy { events: List<EventMessage> ->
                check(events.size == 1) { "Expected 1 event, got ${events.size}" }
                val event = events[0].payload() as OrderItemProductNameCorrected
                check(event.orderId == orderId)
                check(event.productId == productId)
                check(event.oldProductName == "Bad Name")
                check(event.correctedProductName == "Espresso")
            }
    }

    @Test
    fun `should correct null product name with compensating event`() {
        val orderId = "order-1"
        val customerId = "customer-1"
        val productId = "product-1"

        fixture.given().events(
            OrderCreated(id = orderId, customerId = customerId),
            ItemAddedToOrder(
                orderId = orderId,
                productId = productId,
                productName = "",
                quantity = 1,
                price = Money.of(BigDecimal("4.50"), "USD")
            )
        )
            .`when`().command(
                CorrectOrderItemProductName(
                    orderId = orderId,
                    productId = productId,
                    correctedProductName = "Cappuccino"
                )
            )
            .then().success().eventsSatisfy { events: List<EventMessage> ->
                check(events.size == 1) { "Expected 1 event, got ${events.size}" }
                val event = events[0].payload() as OrderItemProductNameCorrected
                check(event.orderId == orderId)
                check(event.productId == productId)
                check(event.oldProductName == "")
                check(event.correctedProductName == "Cappuccino")
            }
    }

    @Test
    fun `should fail to correct product name for non-existent item`() {
        val orderId = "order-1"
        val customerId = "customer-1"

        fixture.given().events(
            OrderCreated(id = orderId, customerId = customerId),
            ItemAddedToOrder(
                orderId = orderId,
                productId = "product-1",
                productName = "Latte",
                quantity = 1,
                price = Money.of(BigDecimal("4.00"), "USD")
            )
        )
            .`when`().command(
                CorrectOrderItemProductName(
                    orderId = orderId,
                    productId = "non-existent-product",
                    correctedProductName = "Espresso"
                )
            )
            .then().exception(IllegalArgumentException::class.java)
    }

    @Test
    fun `should fail to correct product name with blank name`() {
        val orderId = "order-1"
        val customerId = "customer-1"
        val productId = "product-1"

        fixture.given().events(
            OrderCreated(id = orderId, customerId = customerId),
            ItemAddedToOrder(
                orderId = orderId,
                productId = productId,
                productName = "Bad Name",
                quantity = 1,
                price = Money.of(BigDecimal("3.50"), "USD")
            )
        )
            .`when`().command(
                CorrectOrderItemProductName(
                    orderId = orderId,
                    productId = productId,
                    correctedProductName = "   "
                )
            )
            .then().exception(IllegalArgumentException::class.java)
    }

    @Test
    fun `should apply correction during aggregate replay - demonstrating event sourcing`() {
        val orderId = "order-1"
        val customerId = "customer-1"
        val productId = "product-1"

        fixture.given().events(
            OrderCreated(id = orderId, customerId = customerId),
            ItemAddedToOrder(
                orderId = orderId,
                productId = productId,
                productName = "Bad Name",
                quantity = 2,
                price = Money.of(BigDecimal("3.50"), "USD")
            ),
            OrderItemProductNameCorrected(
                orderId = orderId,
                productId = productId,
                oldProductName = "Bad Name",
                correctedProductName = "Espresso",
                correctedAt = Instant.now()
            ),
            OrderSubmitted(orderId = orderId, totalAmount = Money.of(BigDecimal("7.00"), "USD"))
        )
            .`when`().command(DeliverOrder(orderId = orderId))
            .then().success().eventsSatisfy { events: List<EventMessage> ->
                check(events.size == 1) { "Expected 1 event, got ${events.size}" }
                val event = events[0].payload() as OrderDelivered
                check(event.orderId == orderId)
                check(event.items.size == 1)
                check(event.items[0].productId == productId)
                check(event.items[0].productName == "Espresso") { "Expected corrected name 'Espresso', got '${event.items[0].productName}'" }
            }
    }
}
