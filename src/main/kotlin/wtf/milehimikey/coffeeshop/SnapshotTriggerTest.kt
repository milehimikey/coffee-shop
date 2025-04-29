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

/**
 * Test component that generates enough events to trigger snapshots for all aggregate types.
 * This component is only active when the 'snapshot-test' profile is active.
 */
@Component
@Profile("snapshot-test") // Only run in snapshot-test profile
class SnapshotTriggerTest(
    private val commandGateway: CommandGateway,
    private val queryGateway: QueryGateway
) {
    private val logger = LoggerFactory.getLogger(SnapshotTriggerTest::class.java)

    @EventListener(ApplicationStartedEvent::class)
    fun initialize() {
        logger.info("Starting snapshot trigger test...")

        try {
            // Test Order Aggregate Snapshotting (threshold: 50 events)
            testOrderSnapshotting()

            // Test Product Aggregate Snapshotting (threshold: 200 events)
            testProductSnapshotting()

            // Test Payment Aggregate Snapshotting (threshold: 25 events)
            testPaymentSnapshotting()

            logger.info("Snapshot trigger test completed successfully")
            logger.info("You can now check if snapshots were created in the database")

        } catch (e: Exception) {
            logger.error("Error in snapshot trigger test", e)
        }
    }

    private fun testOrderSnapshotting() {
        logger.info("Testing Order aggregate snapshotting...")

        // Create a test product for orders
        val productId = commandGateway.sendAndWait<String>(
            CreateProduct(
                name = "Test Product for Orders",
                description = "Test Product for Order Snapshot Testing",
                price = BigDecimal("1.00")
            )
        )

        // Create a test order
        val orderId = commandGateway.sendAndWait<String>(
            CreateOrder(customerId = "snapshot-test-customer")
        )

        logger.info("Created test order with ID: $orderId")

        // Add 60 items to the order (exceeding our snapshot threshold of 50)
        logger.info("Adding 60 items to the order...")
        for (i in 1..60) {
            commandGateway.sendAndWait<String>(
                AddItemToOrder(
                    orderId = orderId,
                    productId = productId,
                    productName = "Test Product",
                    quantity = 1,
                    price = BigDecimal("1.00")
                )
            )

            if (i % 10 == 0) {
                logger.info("Added $i items to order so far...")
            }
        }

        // Submit the order
        commandGateway.sendAndWait<String>(
            SubmitOrder(orderId = orderId)
        )

        // Deliver the order
        commandGateway.sendAndWait<String>(
            DeliverOrder(orderId = orderId)
        )

        // Complete the order
        commandGateway.sendAndWait<String>(
            CompleteOrder(orderId = orderId)
        )

        logger.info("Order test completed successfully")
    }

    private fun testProductSnapshotting() {
        logger.info("Testing Product aggregate snapshotting...")

        // Create a test product
        val productId = commandGateway.sendAndWait<String>(
            CreateProduct(
                name = "Test Product for Updates",
                description = "Test Product for Product Snapshot Testing",
                price = BigDecimal("10.00")
            )
        )

        logger.info("Created test product with ID: $productId")

        // Update the product 210 times (exceeding our snapshot threshold of 200)
        logger.info("Updating product 210 times...")
        for (i in 1..210) {
            val newPrice = BigDecimal("10.00").add(BigDecimal("0.01").multiply(BigDecimal(i)))

            commandGateway.sendAndWait<String>(
                UpdateProduct(
                    id = productId,
                    name = "Test Product for Updates",
                    description = "Updated ${i} times",
                    price = newPrice
                )
            )

            if (i % 50 == 0) {
                logger.info("Updated product $i times so far...")
            }
        }

        logger.info("Product test completed successfully")
    }

    private fun testPaymentSnapshotting() {
        logger.info("Testing Payment aggregate snapshotting...")

        // Create a test order for payment
        val orderId = commandGateway.sendAndWait<String>(
            CreateOrder(customerId = "snapshot-test-customer-payment")
        )

        // Create a payment
        val paymentId = commandGateway.sendAndWait<String>(
            CreatePayment(
                orderId = orderId,
                amount = BigDecimal("100.00")
            )
        )

        logger.info("Created test payment with ID: $paymentId")

        // Process and refund the payment multiple times to generate events
        logger.info("Processing and refunding payment 15 times...")
        for (i in 1..15) {
            // Process payment
            commandGateway.sendAndWait<String>(
                ProcessPayment(paymentId = paymentId)
            )

            // Refund payment
            commandGateway.sendAndWait<String>(
                RefundPayment(paymentId = paymentId)
            )

            logger.info("Processed and refunded payment $i times")
        }

        // Process one last time and fail it
        commandGateway.sendAndWait<String>(
            ProcessPayment(paymentId = paymentId)
        )

        commandGateway.sendAndWait<String>(
            FailPayment(
                paymentId = paymentId,
                reason = "Test failure reason"
            )
        )

        logger.info("Payment test completed successfully")
    }
}
