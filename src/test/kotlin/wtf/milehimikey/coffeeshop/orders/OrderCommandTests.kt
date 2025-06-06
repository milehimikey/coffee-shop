package wtf.milehimikey.coffeeshop.orders

import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import java.math.BigDecimal

class OrderCommandTests {

    private lateinit var fixture: FixtureConfiguration<Order>

    @BeforeEach
    fun setUp() {
        fixture = AggregateTestFixture(Order::class.java)
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
            price = BigDecimal("3.50")
        )

        val expectedEvent = ItemAddedToOrder(
            orderId = orderId,
            productId = "product-1",
            productName = "Espresso",
            quantity = 2,
            price = BigDecimal("3.50")
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

        val submitCommand = SubmitOrder(orderId = orderId)

        val expectedEvent = OrderSubmitted(
            orderId = orderId,
            totalAmount = BigDecimal("7.00")  // 2 * 3.50 = 7.00
        )

        fixture.given(
            OrderCreated(id = orderId, customerId = customerId),
            ItemAddedToOrder(
                orderId = orderId,
                productId = "product-1",
                productName = "Espresso",
                quantity = 2,
                price = BigDecimal("3.50")
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

        val expectedEvent = OrderDelivered(orderId = orderId)

        fixture.given(
            OrderCreated(id = orderId, customerId = customerId),
            ItemAddedToOrder(
                orderId = orderId,
                productId = "product-1",
                productName = "Espresso",
                quantity = 2,
                price = BigDecimal("3.50")
            ),
            OrderSubmitted(orderId = orderId, totalAmount = BigDecimal("7.00"))
        )
            .`when`(deliverCommand)
            .expectSuccessfulHandlerExecution()
            .expectEvents(expectedEvent)
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
                price = BigDecimal("3.50")
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

        val expectedEvent = OrderCompleted(orderId = orderId)

        fixture.given(
            OrderCreated(id = orderId, customerId = customerId),
            ItemAddedToOrder(
                orderId = orderId,
                productId = "product-1",
                productName = "Espresso",
                quantity = 2,
                price = BigDecimal("3.50")
            ),
            OrderSubmitted(orderId = orderId, totalAmount = BigDecimal("7.00")),
            OrderDelivered(orderId = orderId)
        )
            .`when`(completeCommand)
            .expectSuccessfulHandlerExecution()
            .expectEvents(expectedEvent)
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
                price = BigDecimal("3.50")
            ),
            OrderSubmitted(orderId = orderId, totalAmount = BigDecimal("7.00"))
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
                    price = BigDecimal("1.00")
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
                    totalAmount = BigDecimal("60.00") // 60 items at $1.00 each
                )
            )
    }
}
