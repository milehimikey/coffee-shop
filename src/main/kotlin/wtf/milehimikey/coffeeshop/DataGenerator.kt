package wtf.milehimikey.coffeeshop

import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.messaging.responsetypes.ResponseTypes
import org.axonframework.queryhandling.QueryGateway
import org.javamoney.moneta.Money
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import wtf.milehimikey.coffeeshop.orders.AddItemToOrder
import wtf.milehimikey.coffeeshop.orders.CompleteOrder
import wtf.milehimikey.coffeeshop.orders.CreateOrder
import wtf.milehimikey.coffeeshop.orders.DeliverOrder
import wtf.milehimikey.coffeeshop.orders.FindOrderById
import wtf.milehimikey.coffeeshop.orders.OrderView
import wtf.milehimikey.coffeeshop.orders.SubmitOrder
import wtf.milehimikey.coffeeshop.payments.CreatePayment
import wtf.milehimikey.coffeeshop.payments.FailPayment
import wtf.milehimikey.coffeeshop.payments.ProcessPayment
import wtf.milehimikey.coffeeshop.payments.RefundPayment
import wtf.milehimikey.coffeeshop.payments.ResetPayment
import wtf.milehimikey.coffeeshop.products.CreateProduct
import wtf.milehimikey.coffeeshop.products.FindAllProducts
import wtf.milehimikey.coffeeshop.products.FindProductById
import wtf.milehimikey.coffeeshop.products.ProductView
import wtf.milehimikey.coffeeshop.products.UpdateProduct
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * Component responsible for generating sample data for the coffee shop application.
 * This component can be used to generate data on-demand through REST endpoints.
 */
@Component
class DataGenerator(
    private val commandGateway: CommandGateway,
    private val queryGateway: QueryGateway
) {
    private val logger = LoggerFactory.getLogger(DataGenerator::class.java)

    /**
     * Generates a specified number of products with optional customization.
     *
     * @param count Number of products to generate
     * @param triggerSnapshot Whether to generate enough events to trigger a snapshot for one product
     * @return List of generated product IDs
     */
    fun generateProducts(count: Int, triggerSnapshot: Boolean = false): List<String> {
        logger.info("Generating $count products (triggerSnapshot=$triggerSnapshot)...")

        val productNames = listOf(
            "Espresso", "Cappuccino", "Latte", "Mocha", "Americano",
            "Macchiato", "Flat White", "Cold Brew", "Affogato", "Earl Grey Tea",
            "Green Tea", "Chai Latte", "Croissant", "Blueberry Muffin", "Chocolate Chip Cookie",
            "Caramel Frappuccino", "Iced Coffee", "Hot Chocolate", "Bagel", "Cinnamon Roll",
            "Vanilla Latte", "Pumpkin Spice Latte", "Matcha Latte", "Honey Cake", "Cheesecake"
        )

        val productDescriptions = listOf(
            "A delicious coffee beverage",
            "A customer favorite",
            "Perfect for morning or afternoon",
            "Rich and flavorful",
            "Made with premium ingredients",
            "Freshly prepared",
            "Organic and fair trade",
            "Limited seasonal offering",
            "House specialty",
            "Award-winning recipe"
        )

        val productCommands = (1..count).map { index ->
            val nameIndex = (index - 1) % productNames.size
            val descIndex = (index - 1) % productDescriptions.size
            val basePrice = BigDecimal("2.50").add(BigDecimal((index % 10).toString()))

            CreateProduct(
                name = "${productNames[nameIndex]} #$index",
                description = "${productDescriptions[descIndex]} - Batch #${UUID.randomUUID().toString().substring(0, 8)}",
                price = Money.of(basePrice, "USD")
            )
        }

        // Send commands to create products
        val productIds = productCommands.map { command ->
            commandGateway.sendAndWait<String>(command)
        }

        // If requested, trigger a snapshot for the first product by generating many updates
        if (triggerSnapshot && productIds.isNotEmpty()) {
            val productId = productIds[0]
            logger.info("Triggering snapshot for product $productId by generating 210 updates...")

            // Get the current product details
            val product = queryGateway.query(
                FindProductById(productId),
                ResponseTypes.instanceOf(ProductView::class.java)
            ).get()

            if (product != null) {
                var currentPrice = product.price
                var currentName = product.name

                // Perform 210 updates (exceeding the snapshot threshold of 200)
                for (i in 1..210) {
                    // Small price fluctuation (±$0.05)
                    val priceChange = Money.of(if (i % 2 == 0) BigDecimal("0.05") else BigDecimal("-0.05"), "USD")
                    currentPrice = currentPrice.add(priceChange)

                    // Ensure price doesn't go below $2.00 or above $10.00
                    val minPrice = Money.of(BigDecimal("2.00"), "USD")
                    val maxPrice = Money.of(BigDecimal("10.00"), "USD")
                    if (currentPrice.isLessThan(minPrice)) {
                        currentPrice = minPrice
                    } else if (currentPrice.isGreaterThan(maxPrice)) {
                        currentPrice = maxPrice
                    }

                    commandGateway.sendAndWait<String>(
                        UpdateProduct(
                            id = productId,
                            name = currentName,
                            description = "${product.description} - Update #$i at ${Instant.now()}",
                            price = currentPrice
                        )
                    )

                    if (i % 50 == 0) {
                        logger.info("Updated product $i times")
                    }
                }

                logger.info("Completed 210 updates for product $productId, snapshot should be triggered")
            }
        }

        logger.info("Generated ${productIds.size} products")
        return productIds
    }

    /**
     * Generates a specified number of orders with optional customization.
     *
     * @param count Number of orders to generate
     * @param customerId Optional customer ID to use for all orders (random if not specified)
     * @param itemsPerOrder Number of items to add to each order (random 1-5 if not specified)
     * @param triggerSnapshot Whether to generate enough events to trigger a snapshot for one order
     * @param completeOrders Whether to complete the orders (submit, deliver, complete)
     * @return List of generated order IDs
     */
    fun generateOrders(
        count: Int,
        customerId: String? = null,
        itemsPerOrder: Int? = null,
        triggerSnapshot: Boolean = false,
        completeOrders: Boolean = true
    ): List<String> {
        logger.info("Generating $count orders (triggerSnapshot=$triggerSnapshot, completeOrders=$completeOrders)...")

        // Get available products
        val products = queryGateway.query(
            FindAllProducts(includeInactive = false),
            ResponseTypes.multipleInstancesOf(ProductView::class.java)
        ).get()

        if (products.isEmpty()) {
            logger.warn("No products available. Generating 10 products first...")
            generateProducts(10, false)

            // Query again for products
            val newProducts = queryGateway.query(
                FindAllProducts(includeInactive = false),
                ResponseTypes.multipleInstancesOf(ProductView::class.java)
            ).get()

            if (newProducts.isEmpty()) {
                throw IllegalStateException("Failed to generate products")
            }

            return generateOrders(count, customerId, itemsPerOrder, triggerSnapshot, completeOrders)
        }

        // Generate customer IDs if not provided
        val customerIds = if (customerId != null) {
            listOf(customerId)
        } else {
            (1..15).map { "customer-$it" }
        }

        val orderIds = mutableListOf<String>()

        // Create orders
        for (i in 1..count) {
            val orderCustomerId = customerIds.random()

            // Create order
            val orderId = commandGateway.sendAndWait<String>(
                CreateOrder(customerId = orderCustomerId)
            )

            // Determine how many items to add
            val numItems = if (triggerSnapshot && i == 1) {
                // For the first order when triggering snapshot, add 60 items (exceeding threshold of 50)
                60
            } else if (itemsPerOrder != null) {
                itemsPerOrder
            } else {
                (1..5).random()
            }

            // Add items to the order
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

                // Log progress for large orders
                if (numItems > 20 && j % 20 == 0) {
                    logger.info("Added $j items to order $orderId so far...")
                }
            }

            if (completeOrders) {
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
            }

            orderIds.add(orderId)

            // Log progress
            if (i % 50 == 0 || i == count) {
                logger.info("Generated $i orders so far...")
            }
        }

        logger.info("Generated ${orderIds.size} orders")
        return orderIds
    }

    /**
     * Generates payments for the specified orders.
     *
     * @param orderIds List of order IDs to generate payments for
     * @param triggerSnapshot Whether to generate enough events to trigger a snapshot for one payment
     * @param triggerDeadLetter Whether to generate a payment that will trigger a dead letter
     * @return List of generated payment IDs
     */
    fun generatePayments(
        orderIds: List<String>,
        triggerSnapshot: Boolean = false,
        triggerDeadLetter: Boolean = false
    ): List<String> {
        logger.info("Generating payments for ${orderIds.size} orders (triggerSnapshot=$triggerSnapshot, triggerDeadLetter=$triggerDeadLetter)...")

        val paymentIds = mutableListOf<String>()
        var processedCount = 0

        // If requested, create a special payment with amount $13.13 to trigger dead letter
        if (triggerDeadLetter && orderIds.isNotEmpty()) {
            val specialOrderId = orderIds.first()
            logger.info("Creating a special payment with amount $13.13 to demonstrate dead letter queue functionality")

            val specialPaymentId = commandGateway.sendAndWait<String>(
                CreatePayment(
                    orderId = specialOrderId,
                    amount = BigDecimal("13.13")
                )
            )

            // Process the special payment
            commandGateway.sendAndWait<String>(
                ProcessPayment(paymentId = specialPaymentId)
            )

            // Attempt to reset the payment - this will trigger the dead letter
            try {
                commandGateway.sendAndWait<String>(
                    ResetPayment(paymentId = specialPaymentId)
                )
            } catch (e: Exception) {
                logger.info("Expected error when resetting payment with amount $13.13: ${e.message}")
                logger.info("This payment should be sent to the dead letter queue")
            }

            paymentIds.add(specialPaymentId)
            processedCount++
        }

        // Process the remaining orders
        for ((index, orderId) in orderIds.withIndex()) {
            // Skip the first order if we already created a special payment for it
            if (triggerDeadLetter && index == 0) continue

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
                        amount = order.totalAmount!!.number.numberValue(BigDecimal::class.java)
                    )
                )

                // If requested, trigger snapshot for the first payment by generating many events
                if (triggerSnapshot && index == (if (triggerDeadLetter) 1 else 0)) {
                    logger.info("Creating multiple events for payment $paymentId to trigger snapshotting")

                    // Process and refund the payment multiple times to generate events
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

                // Log progress
                if (processedCount % 50 == 0 || processedCount == orderIds.size) {
                    logger.info("Created $processedCount payments so far...")
                }
            }
        }

        logger.info("Generated ${paymentIds.size} payments")
        return paymentIds
    }

    /**
     * Generates a complete batch of data including products, orders, and payments.
     *
     * @param productCount Number of products to generate
     * @param orderCount Number of orders to generate
     * @param triggerSnapshots Whether to trigger snapshots for each aggregate type
     * @param triggerDeadLetters Whether to trigger dead letters
     * @return Summary of generated data
     */
    fun generateBatch(
        productCount: Int = 10,
        orderCount: Int = 50,
        triggerSnapshots: Boolean = true,
        triggerDeadLetters: Boolean = true
    ): BatchGenerationResult {
        logger.info("Generating batch: $productCount products, $orderCount orders, triggerSnapshots=$triggerSnapshots, triggerDeadLetters=$triggerDeadLetters")

        // Generate products
        val productIds = generateProducts(productCount, triggerSnapshots)

        // Generate orders
        val orderIds = generateOrders(orderCount, null, null, triggerSnapshots, true)

        // Generate payments
        val paymentIds = generatePayments(orderIds, triggerSnapshots, triggerDeadLetters)

        return BatchGenerationResult(
            productCount = productIds.size,
            orderCount = orderIds.size,
            paymentCount = paymentIds.size,
            snapshotsTriggered = triggerSnapshots,
            deadLettersTriggered = triggerDeadLetters
        )
    }

    /**
     * Triggers dead letters for all event processors.
     *
     * @return Summary of triggered dead letters
     */
    fun triggerDeadLetters(): DeadLetterTriggerResult {
        logger.info("Triggering dead letters for all processors...")

        val results = mutableMapOf<String, String>()

        // Trigger dead letter for payment processor
        results["payment"] = triggerPaymentDeadLetter()

        // Trigger dead letter for product processor
        results["product"] = triggerProductDeadLetter()

        // Trigger dead letter for order processor
        results["order"] = triggerOrderDeadLetter()

        logger.info("Dead letter triggering completed")
        return DeadLetterTriggerResult(results)
    }

    /**
     * Triggers a dead letter for the payment processor.
     *
     * @return Result message
     */
    fun triggerPaymentDeadLetter(): String {
        logger.info("Triggering dead letter for payment processor...")

        try {
            // Create a payment with the special amount that triggers errors
            val orderId = commandGateway.sendAndWait<String>(
                CreateOrder(customerId = "dead-letter-test")
            )

            // Add an item to the order
            commandGateway.sendAndWait<String>(
                AddItemToOrder(
                    orderId = orderId,
                    productId = "test-product",
                    productName = "Test Product",
                    quantity = 1,
                    price = Money.of(BigDecimal("13.13"), "USD")
                )
            )

            // Submit the order
            commandGateway.sendAndWait<String>(
                SubmitOrder(orderId = orderId)
            )

            // Create payment with the special amount
            val paymentId = commandGateway.sendAndWait<String>(
                CreatePayment(
                    orderId = orderId,
                    amount = BigDecimal("13.13")
                )
            )

            // Process the payment
            commandGateway.sendAndWait<String>(
                ProcessPayment(paymentId = paymentId)
            )

            // Attempt to reset the payment - this will trigger the dead letter
            try {
                commandGateway.sendAndWait<String>(
                    ResetPayment(paymentId = paymentId)
                )
            } catch (e: Exception) {
                logger.info("Expected error when resetting payment with amount $13.13: ${e.message}")
                return "Dead letter triggered for payment processor with payment ID: $paymentId"
            }

            return "Payment reset did not trigger expected error for payment ID: $paymentId"
        } catch (e: Exception) {
            logger.error("Failed to trigger dead letter for payment processor", e)
            return "Failed to trigger dead letter for payment processor: ${e.message}"
        }
    }

    /**
     * Triggers a dead letter for the product processor.
     *
     * @return Result message
     */
    fun triggerProductDeadLetter(): String {
        logger.info("Triggering dead letter for product processor...")

        try {
            // Create a product with the special price that triggers errors
            val productId = commandGateway.sendAndWait<String>(
                CreateProduct(
                    name = "Error Triggering Product",
                    description = "This product will trigger a dead letter when updated",
                    price = Money.of(BigDecimal("9.99"), "USD") // Start with a normal price
                )
            )

            // Update the product with the error-triggering price
            try {
                commandGateway.sendAndWait<String>(
                    UpdateProduct(
                        id = productId,
                        name = "Error Triggering Product",
                        description = "This product has been updated with an error-triggering price",
                        price = Money.of(BigDecimal("99.99"), "USD") // This price triggers the error
                    )
                )
            } catch (e: Exception) {
                logger.info("Expected error when updating product with price $99.99: ${e.message}")
                return "Dead letter triggered for product processor with product ID: $productId"
            }

            return "Product update did not trigger expected error for product ID: $productId"
        } catch (e: Exception) {
            logger.error("Failed to trigger dead letter for product processor", e)
            return "Failed to trigger dead letter for product processor: ${e.message}"
        }
    }

    /**
     * Triggers a dead letter for the order processor.
     *
     * @return Result message
     */
    fun triggerOrderDeadLetter(): String {
        logger.info("Triggering dead letter for order processor...")

        try {
            // Create an order with the special customer ID that triggers errors
            try {
                val orderId = commandGateway.sendAndWait<String>(
                    CreateOrder(customerId = "error-customer")
                )

                return "Order creation did not trigger expected error for customer ID: error-customer"
            } catch (e: Exception) {
                logger.info("Expected error when creating order with customer ID 'error-customer': ${e.message}")
                return "Dead letter triggered for order processor with customer ID: error-customer"
            }
        } catch (e: Exception) {
            logger.error("Failed to trigger dead letter for order processor", e)
            return "Failed to trigger dead letter for order processor: ${e.message}"
        }
    }
}

/**
 * Result of batch data generation.
 */
data class BatchGenerationResult(
    val productCount: Int,
    val orderCount: Int,
    val paymentCount: Int,
    val snapshotsTriggered: Boolean,
    val deadLettersTriggered: Boolean
)

/**
 * Result of dead letter triggering.
 */
data class DeadLetterTriggerResult(
    val results: Map<String, String>
)
