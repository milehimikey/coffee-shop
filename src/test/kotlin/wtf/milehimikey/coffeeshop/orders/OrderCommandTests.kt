package wtf.milehimikey.coffeeshop.orders

import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
}
