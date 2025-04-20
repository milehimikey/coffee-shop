package wtf.milehimikey.coffeeshop

import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.messaging.responsetypes.ResponseTypes
import org.axonframework.queryhandling.QueryGateway
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import wtf.milehimikey.coffeeshop.orders.*
import wtf.milehimikey.coffeeshop.payments.*
import wtf.milehimikey.coffeeshop.products.*
import java.math.BigDecimal
import java.util.concurrent.CompletableFuture

@Component
@Profile("!test") // Don't run in test profile
class DataInitializer(
    private val commandGateway: CommandGateway,
    private val queryGateway: QueryGateway
) {
    private val logger = LoggerFactory.getLogger(DataInitializer::class.java)
    
    @EventListener(ApplicationStartedEvent::class)
    fun initialize() {
        logger.info("Initializing sample data...")
        
        try {
            // Create sample products
            val products = createSampleProducts()
            
            // Create sample customers
            val customers = listOf("customer-1", "customer-2", "customer-3")
            
            // Create sample orders
            val orders = createSampleOrders(customers, products)
            
            // Create sample payments
            createSamplePayments(orders)
            
            logger.info("Sample data initialization completed successfully")
        } catch (e: Exception) {
            logger.error("Error initializing sample data", e)
        }
    }
    
    private fun createSampleProducts(): List<ProductView> {
        val productCommands = listOf(
            CreateProduct(
                name = "Espresso",
                description = "Strong coffee brewed by forcing hot water under pressure through finely ground coffee beans",
                price = BigDecimal("3.50")
            ),
            CreateProduct(
                name = "Cappuccino",
                description = "Coffee drink with espresso, hot milk, and steamed milk foam",
                price = BigDecimal("4.50")
            ),
            CreateProduct(
                name = "Latte",
                description = "Coffee drink made with espresso and steamed milk",
                price = BigDecimal("4.00")
            ),
            CreateProduct(
                name = "Mocha",
                description = "Chocolate-flavored variant of a latte",
                price = BigDecimal("4.75")
            ),
            CreateProduct(
                name = "Americano",
                description = "Espresso diluted with hot water",
                price = BigDecimal("3.75")
            ),
            CreateProduct(
                name = "Macchiato",
                description = "Espresso coffee drink with a small amount of milk",
                price = BigDecimal("4.25")
            )
        )
        
        // Send commands to create products
        val productIds = productCommands.map { command ->
            commandGateway.sendAndWait<String>(command)
        }
        
        logger.info("Created ${productIds.size} sample products")
        
        // Wait a bit for event processing
        Thread.sleep(1000)
        
        // Query for the created products
        return queryGateway.query(
            FindAllProducts(includeInactive = false),
            ResponseTypes.multipleInstancesOf(ProductView::class.java)
        ).get()
    }
    
    private fun createSampleOrders(customers: List<String>, products: List<ProductView>): List<String> {
        val orderIds = mutableListOf<String>()
        
        // Create 5 sample orders
        for (i in 1..5) {
            val customerId = customers.random()
            
            // Create order
            val orderId = commandGateway.sendAndWait<String>(
                CreateOrder(customerId = customerId)
            )
            
            // Add 1-3 random products to the order
            val numItems = (1..3).random()
            for (j in 1..numItems) {
                val product = products.random()
                val quantity = (1..3).random()
                
                commandGateway.sendAndWait<String>(
                    AddItemToOrder(
                        orderId = orderId,
                        productId = product.id,
                        productName = product.name,
                        quantity = quantity,
                        price = product.price
                    )
                )
            }
            
            // Submit the order
            commandGateway.sendAndWait<String>(
                SubmitOrder(orderId = orderId)
            )
            
            // For some orders, mark them as delivered
            if (i % 2 == 0) {
                commandGateway.sendAndWait<String>(
                    DeliverOrder(orderId = orderId)
                )
                
                // For some delivered orders, mark them as completed
                if (i % 4 == 0) {
                    commandGateway.sendAndWait<String>(
                        CompleteOrder(orderId = orderId)
                    )
                }
            }
            
            orderIds.add(orderId)
        }
        
        logger.info("Created ${orderIds.size} sample orders")
        return orderIds
    }
    
    private fun createSamplePayments(orderIds: List<String>) {
        val paymentIds = mutableListOf<String>()
        
        // Create payments for each order
        for (orderId in orderIds) {
            // Query for the order to get the total amount
            val order = queryGateway.query(
                FindOrderById(orderId),
                ResponseTypes.instanceOf(OrderView::class.java)
            ).get()
            
            if (order != null) {
                // Create payment
                val paymentId = commandGateway.sendAndWait<String>(
                    CreatePayment(
                        orderId = orderId,
                        amount = order.totalAmount
                    )
                )
                
                // Process most payments
                if (Math.random() > 0.2) {
                    commandGateway.sendAndWait<String>(
                        ProcessPayment(paymentId = paymentId)
                    )
                    
                    // Refund some payments
                    if (Math.random() > 0.8) {
                        commandGateway.sendAndWait<String>(
                            RefundPayment(paymentId = paymentId)
                        )
                    }
                } else {
                    // Fail some payments
                    commandGateway.sendAndWait<String>(
                        FailPayment(
                            paymentId = paymentId,
                            reason = "Insufficient funds"
                        )
                    )
                }
                
                paymentIds.add(paymentId)
            }
        }
        
        logger.info("Created ${paymentIds.size} sample payments")
    }
}
