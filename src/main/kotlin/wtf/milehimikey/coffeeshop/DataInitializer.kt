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
            val customers = listOf(
                "customer-1", "customer-2", "customer-3", "customer-4", "customer-5",
                "customer-6", "customer-7", "customer-8", "customer-9", "customer-10",
                "customer-11", "customer-12", "customer-13", "customer-14", "customer-15"
            )

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
            // Coffee drinks
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
            ),
            CreateProduct(
                name = "Flat White",
                description = "Coffee drink consisting of espresso with microfoam",
                price = BigDecimal("4.50")
            ),
            CreateProduct(
                name = "Cold Brew",
                description = "Coffee made by steeping coffee grounds in cold water for an extended period",
                price = BigDecimal("4.75")
            ),
            CreateProduct(
                name = "Affogato",
                description = "Coffee-based dessert with espresso poured over ice cream",
                price = BigDecimal("5.50")
            ),

            // Tea drinks
            CreateProduct(
                name = "Earl Grey Tea",
                description = "Black tea flavored with oil of bergamot",
                price = BigDecimal("3.25")
            ),
            CreateProduct(
                name = "Green Tea",
                description = "Tea made from Camellia sinensis leaves that have not undergone oxidation",
                price = BigDecimal("3.25")
            ),
            CreateProduct(
                name = "Chai Latte",
                description = "Spiced tea mixed with steamed milk",
                price = BigDecimal("4.25")
            ),

            // Pastries and snacks
            CreateProduct(
                name = "Croissant",
                description = "Buttery, flaky pastry of Austrian origin",
                price = BigDecimal("3.25")
            ),
            CreateProduct(
                name = "Blueberry Muffin",
                description = "Sweet quickbread muffin with blueberries",
                price = BigDecimal("3.50")
            ),
            CreateProduct(
                name = "Chocolate Chip Cookie",
                description = "Sweet baked treat with chocolate chips",
                price = BigDecimal("2.75")
            )
        )

        // Send commands to create products
        val productIds = productCommands.map { command ->
            commandGateway.sendAndWait<String>(command)
        }

        // For the first product (Espresso), perform many updates to trigger snapshotting
        if (productIds.isNotEmpty()) {
            val espressoId = productIds[0]
            logger.info("Performing multiple price updates on Espresso product to trigger snapshotting")

            // Start with base price of $3.50
            var currentPrice = BigDecimal("3.50")

            // Perform 210 updates (exceeding the snapshot threshold of 200)
            for (i in 1..210) {
                // Small price fluctuation (±$0.05)
                val priceChange = if (i % 2 == 0) BigDecimal("0.05") else BigDecimal("-0.05")
                currentPrice = currentPrice.add(priceChange)

                // Ensure price doesn't go below $3.00 or above $4.00
                if (currentPrice < BigDecimal("3.00")) {
                    currentPrice = BigDecimal("3.00")
                } else if (currentPrice > BigDecimal("4.00")) {
                    currentPrice = BigDecimal("4.00")
                }

                commandGateway.sendAndWait<String>(
                    UpdateProduct(
                        id = espressoId,
                        name = "Espresso",
                        description = "Strong coffee brewed by forcing hot water under pressure through finely ground coffee beans. Price update #$i",
                        price = currentPrice
                    )
                )

                if (i % 50 == 0) {
                    logger.info("Updated Espresso price $i times")
                }
            }

            logger.info("Completed price updates for Espresso product")
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

        // Create 300 sample orders
        logger.info("Starting to create 300 sample orders...")
        for (i in 1..300) {
            val customerId = customers.random()

            // Create order
            val orderId = commandGateway.sendAndWait<String>(
                CreateOrder(customerId = customerId)
            )

            // For a few orders, add many items to trigger snapshotting
            if (i % 50 == 0) {
                // Add 60 items to trigger snapshot (threshold is 50)
                logger.info("Creating a large order with 60 items for order $orderId")
                for (j in 1..60) {
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
            } else {
                // Add 1-5 random products to the order (normal case)
                val numItems = (1..5).random()
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

            // Log progress every 50 orders
            if (i % 50 == 0) {
                logger.info("Created $i orders so far...")
            }
        }

        logger.info("Created ${orderIds.size} sample orders")
        return orderIds
    }

    private fun createSamplePayments(orderIds: List<String>) {
        val paymentIds = mutableListOf<String>()

        // Create payments for each order
        logger.info("Starting to create payments for ${orderIds.size} orders...")
        var processedCount = 0

        for ((index, orderId) in orderIds.withIndex()) {
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

                // For every 50th payment, create many events to trigger snapshotting
                if (index % 50 == 0) {
                    logger.info("Creating multiple events for payment $paymentId to trigger snapshotting")

                    // Process and refund the payment multiple times to generate events
                    // We'll use the ResetPayment command to reset the payment status to PENDING
                    // after each process-refund cycle

                    // First process the payment
                    commandGateway.sendAndWait<String>(
                        ProcessPayment(paymentId = paymentId)
                    )

                    // Generate 30+ events for this payment (exceeding threshold of 25)
                    for (i in 1..15) {
                        // Refund the payment
                        commandGateway.sendAndWait<String>(
                            RefundPayment(paymentId = paymentId)
                        )

                        // Reset the payment to PENDING status
                        commandGateway.sendAndWait<String>(
                            ResetPayment(paymentId = paymentId)
                        )

                        // Process the payment again
                        commandGateway.sendAndWait<String>(
                            ProcessPayment(paymentId = paymentId)
                        )

                        if (i % 5 == 0) {
                            logger.info("Processed and refunded payment $i times")
                        }
                    }

                    logger.info("Created 30+ events for payment $paymentId")
                } else {
                    // Process most payments (normal case)
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
                }

                paymentIds.add(paymentId)
                processedCount++

                // Log progress every 50 payments
                if (processedCount % 50 == 0) {
                    logger.info("Created $processedCount payments so far...")
                }
            }
        }

        logger.info("Created ${paymentIds.size} sample payments")
    }
}
