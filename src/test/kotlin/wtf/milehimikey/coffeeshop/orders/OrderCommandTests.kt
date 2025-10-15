package wtf.milehimikey.coffeeshop.orders

import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import org.axonframework.test.matchers.Matchers.exactSequenceOf
import org.axonframework.test.matchers.Matchers.payloadsMatching
import org.axonframework.test.matchers.Matchers.predicate
import org.javamoney.moneta.Money
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant

class OrderCommandTests {

    private lateinit var fixture: FixtureConfiguration<Order>
    private val orderTotalCalculator = mock(OrderTotalCalculator::class.java)

    @BeforeEach
    fun setUp() {
        fixture = AggregateTestFixture(Order::class.java)
        fixture.registerInjectableResource(orderTotalCalculator)
    }

    @Test
    fun `should create order`() {
        val command = CreateOrder(
            id = "order-1",
            customerId = "customer-1"
        )

        val expectedEvent = OrderCreated(
            id = "order-1",
            customerId = "customer-1"
        )

        fixture.givenNoPriorActivity()
            .`when`(command)
            .expectSuccessfulHandlerExecution()
            .expectEvents(expectedEvent)
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

        fixture.given(OrderCreated(id = orderId, customerId = customerId))
            .`when`(addItemCommand)
            .expectSuccessfulHandlerExecution()
            .expectEvents(expectedEvent)
    }

    @Test
    fun `should submit order`() {
        val orderId = "order-1"
        val customerId = "customer-1"
        `when`(orderTotalCalculator.calculateTotal(anyList())).thenReturn(
            Money.of(BigDecimal("7.00"), "USD")
        )

        val submitCommand = SubmitOrder(orderId = orderId)

        val expectedEvent = OrderSubmitted(
            orderId = orderId,
            totalAmount =  Money.of(BigDecimal("7.00"), "USD")
        )

        fixture.given(
            OrderCreated(id = orderId, customerId = customerId),
            ItemAddedToOrder(
                orderId = orderId,
                productId = "product-1",
                productName = "Espresso",
                quantity = 2,
                price = Money.of(BigDecimal("3.50"), "USD")
            )
        )
            .`when`(submitCommand)
            .expectSuccessfulHandlerExecution()
            .expectEvents(expectedEvent)
    }

    @Test
    fun `should not submit empty order`() {
        val orderId = "order-1"
        val customerId = "customer-1"

        val submitCommand = SubmitOrder(orderId = orderId)

        fixture.given(OrderCreated(id = orderId, customerId = customerId))
            .`when`(submitCommand)
            .expectException(IllegalStateException::class.java)
    }

    @Test
    fun `should deliver order`() {
        val orderId = "order-1"
        val customerId = "customer-1"

        val deliverCommand = DeliverOrder(orderId = orderId)

        fixture.given(
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
            .`when`(deliverCommand)
            .expectSuccessfulHandlerExecution()
            .expectEventsMatching(payloadsMatching(exactSequenceOf(
                predicate<OrderDelivered> { event ->
                    event.orderId == orderId &&
                    event.customerId == customerId &&
                    event.items.size == 1 &&
                    event.items[0].productId == "product-1" &&
                    event.items[0].productName == "Espresso" &&
                    event.items[0].quantity == 2 &&
                    event.totalAmount == Money.of(BigDecimal("7.00"), "USD")
                }
            )))
    }

    @Test
    fun `should not deliver unsubmitted order`() {
        val orderId = "order-1"
        val customerId = "customer-1"

        val deliverCommand = DeliverOrder(orderId = orderId)

        fixture.given(
            OrderCreated(id = orderId, customerId = customerId),
            ItemAddedToOrder(
                orderId = orderId,
                productId = "product-1",
                productName = "Espresso",
                quantity = 2,
                price = Money.of(BigDecimal("3.50"), "USD")
            )
        )
            .`when`(deliverCommand)
            .expectException(IllegalStateException::class.java)
    }

    @Test
    fun `should complete order`() {
        val orderId = "order-1"
        val customerId = "customer-1"

        val completeCommand = CompleteOrder(orderId = orderId)

        fixture.given(
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
            .`when`(completeCommand)
            .expectSuccessfulHandlerExecution()
            .expectEventsMatching(payloadsMatching(exactSequenceOf(
                predicate<OrderCompleted> { event ->
                    event.orderId == orderId &&
                    event.customerId == customerId &&
                    event.items.size == 1 &&
                    event.items[0].productId == "product-1" &&
                    event.items[0].productName == "Espresso" &&
                    event.items[0].quantity == 2 &&
                    event.totalAmount == Money.of(BigDecimal("7.00"), "USD")
                }
            )))
    }

    @Test
    fun `should not complete undelivered order`() {
        val orderId = "order-1"
        val customerId = "customer-1"

        val completeCommand = CompleteOrder(orderId = orderId)

        fixture.given(
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
            .`when`(completeCommand)
            .expectException(IllegalStateException::class.java)
    }

    /**
     * This test demonstrates the behavior of an aggregate with many events,
     * which would benefit from snapshotting in a production environment.
     *
     * Note: The AggregateTestFixture doesn't actually create snapshots during testing,
     * so this test is more for demonstration purposes. In a real application,
     * snapshots would be created after the configured threshold (50 events).
     */
    @Test
    @Disabled("This test is for demonstration purposes only and may take a long time to run")
    fun `should handle many events which would benefit from snapshotting`() {
        val orderId = "order-1"
        val customerId = "customer-1"

        // First create the order
        val events = mutableListOf<Any>(
            OrderCreated(id = orderId, customerId = customerId)
        )

        // Add 60 items to the order (not enough to exceed our snapshot threshold of 200, but enough for demonstration)
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

        // Submit the order
        val submitCommand = SubmitOrder(orderId = orderId)

        // In a real application, a snapshot would be created after 200 events
        // and used to load the aggregate state more efficiently
        fixture.given(events)
            .`when`(submitCommand)
            .expectSuccessfulHandlerExecution()
            .expectEvents(
                OrderSubmitted(
                    orderId = orderId,
                    totalAmount = Money.of(BigDecimal("60.00"), "USD")
                )
            )
    }

}
