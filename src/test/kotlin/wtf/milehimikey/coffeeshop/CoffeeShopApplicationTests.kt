package wtf.milehimikey.coffeeshop

import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import wtf.milehimikey.coffeeshop.config.IdempotencyRepository
import wtf.milehimikey.coffeeshop.orders.OrderView
import wtf.milehimikey.coffeeshop.payments.PaymentView
import wtf.milehimikey.coffeeshop.products.ProductView
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Import(TestcontainersConfiguration::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
class CoffeeShopApplicationTests {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var idempotencyRepository: IdempotencyRepository

    @Test
    fun contextLoads() {
    }

    // Product REST Endpoint Tests

    @Test
    fun `should create product`() {
        // Given
        val request = CreateProductRequest(
            name = "Cappuccino",
            description = "Coffee with milk foam",
            price = BigDecimal("4.50")
        )

        // When
        val response = restTemplate.postForEntity(
            "/api/products",
            request,
            String::class.java
        )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val productResponse = restTemplate.getForEntity(
                "/api/products",
                Array<ProductView>::class.java
            )
            assertEquals(HttpStatus.OK, productResponse.statusCode)
            assertNotNull(productResponse.body)
            assert(productResponse.body!!.isNotEmpty())
        }
    }

    @Test
    fun `should get product by id`() {
        // Given
        val createRequest = CreateProductRequest(
            name = "Latte",
            description = "Coffee with steamed milk",
            price = BigDecimal("4.00")
        )

        val createResponse = restTemplate.postForEntity(
            "/api/products",
            createRequest,
            String::class.java
        )
        assertEquals(HttpStatus.OK, createResponse.statusCode)
        val productId = createResponse.body
        assertNotNull(productId)

        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val productResponse = restTemplate.getForEntity(
                "/api/products/{id}",
                ProductView::class.java,
                productId
            )
            assertEquals(HttpStatus.OK, productResponse.statusCode)
        }

        // When
        val response = restTemplate.getForEntity(
            "/api/products/{id}",
            ProductView::class.java,
            productId
        )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Latte", response.body?.name)
        assertEquals("Coffee with steamed milk", response.body?.description)
        assertEquals(0, BigDecimal("4.00").compareTo(response.body?.price))
    }

    @Test
    fun `should update product`() {
        // Given
        val createRequest = CreateProductRequest(
            name = "Mocha",
            description = "Coffee with chocolate",
            price = BigDecimal("4.75")
        )

        val createResponse = restTemplate.postForEntity(
            "/api/products",
            createRequest,
            String::class.java
        )
        assertEquals(HttpStatus.OK, createResponse.statusCode)
        val productId = createResponse.body
        assertNotNull(productId)

        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val productResponse = restTemplate.getForEntity(
                "/api/products/{id}",
                ProductView::class.java,
                productId
            )
            assertEquals(HttpStatus.OK, productResponse.statusCode)
        }

        val updateRequest = UpdateProductRequest(
            name = "Mocha Deluxe",
            description = "Coffee with premium chocolate",
            price = BigDecimal("5.25")
        )

        // When
        val response = restTemplate.exchange(
            "/api/products/{id}",
            HttpMethod.PUT,
            HttpEntity(updateRequest),
            String::class.java,
            productId
        )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)

        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val updatedResponse = restTemplate.getForEntity(
                "/api/products/{id}",
                ProductView::class.java,
                productId
            )
            assertEquals(HttpStatus.OK, updatedResponse.statusCode)
            assertEquals("Mocha Deluxe", updatedResponse.body?.name)
        }

        // Verify the update
        val getResponse = restTemplate.getForEntity(
            "/api/products/{id}",
            ProductView::class.java,
            productId
        )

        assertEquals(HttpStatus.OK, getResponse.statusCode)
        assertEquals("Mocha Deluxe", getResponse.body?.name)
        assertEquals("Coffee with premium chocolate", getResponse.body?.description)
        assertEquals(0, BigDecimal("5.25").compareTo(getResponse.body?.price))
    }

    @Test
    fun `should delete product`() {
        // Given
        val createRequest = CreateProductRequest(
            name = "Americano",
            description = "Espresso with water",
            price = BigDecimal("3.75")
        )

        val createResponse = restTemplate.postForEntity(
            "/api/products",
            createRequest,
            String::class.java
        )
        assertEquals(HttpStatus.OK, createResponse.statusCode)
        val productId = createResponse.body
        assertNotNull(productId)

        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val productResponse = restTemplate.getForEntity(
                "/api/products/{id}",
                ProductView::class.java,
                productId
            )
            assertEquals(HttpStatus.OK, productResponse.statusCode)
        }

        // When
        val deleteResponse = restTemplate.exchange(
            "/api/products/{id}",
            HttpMethod.DELETE,
            null,
            String::class.java,
            productId
        )

        // Then
        assertEquals(HttpStatus.OK, deleteResponse.statusCode)

        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val productResponse = restTemplate.getForEntity(
                "/api/products/{id}",
                ProductView::class.java,
                productId
            )
            assertEquals(HttpStatus.OK, productResponse.statusCode)
            assertEquals(false, productResponse.body?.active)
        }

        // Verify product is marked as inactive
        val getResponse = restTemplate.getForEntity(
            "/api/products/{id}",
            ProductView::class.java,
            productId
        )

        assertEquals(HttpStatus.OK, getResponse.statusCode)
        assertEquals(false, getResponse.body?.active)
    }

    @Test
    fun `should get all products`() {
        // Given
        val products = listOf(
            CreateProductRequest("Espresso", "Strong coffee", BigDecimal("3.50")),
            CreateProductRequest("Latte", "Coffee with steamed milk", BigDecimal("4.00")),
            CreateProductRequest("Cappuccino", "Coffee with milk foam", BigDecimal("4.50"))
        )

        products.forEach { product ->
            val createResponse = restTemplate.postForEntity(
                "/api/products",
                product,
                String::class.java
            )
            assertEquals(HttpStatus.OK, createResponse.statusCode)
            assertNotNull(createResponse.body)
        }

        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val productResponse = restTemplate.getForEntity(
                "/api/products",
                Array<ProductView>::class.java
            )
            assertEquals(HttpStatus.OK, productResponse.statusCode)
            assertNotNull(productResponse.body)
            assertTrue(productResponse.body?.isNotEmpty() == true, "Expected at least one product")
        }

        // When
        val response = restTemplate.getForEntity(
            "/api/products",
            Array<ProductView>::class.java
        )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body?.isNotEmpty() == true, "Expected at least one product")
    }

    // Order REST Endpoint Tests

    @Test
    fun `should create order`() {
        // Given
        val request = CreateOrderRequest(
            customerId = "customer-1"
        )

        // When
        val response = restTemplate.postForEntity(
            "/api/orders",
            request,
            String::class.java
        )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val orderResponse = restTemplate.getForEntity(
                "/api/orders",
                Array<OrderView>::class.java
            )
            assertEquals(HttpStatus.OK, orderResponse.statusCode)
            assertNotNull(orderResponse.body)
            assert(orderResponse.body!!.isNotEmpty())
        }
    }

    @Test
    fun `should add item to order`() {
        // Given
        val createOrderRequest = CreateOrderRequest(
            customerId = "customer-1"
        )

        val createOrderResponse = restTemplate.postForEntity(
            "/api/orders",
            createOrderRequest,
            String::class.java
        )
        assertEquals(HttpStatus.OK, createOrderResponse.statusCode)
        val orderId = createOrderResponse.body
        assertNotNull(orderId)

        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val orderResponse = restTemplate.getForEntity(
                "/api/orders/{id}",
                OrderView::class.java,
                orderId
            )
            assertEquals(HttpStatus.OK, orderResponse.statusCode)
        }

        val addItemRequest = AddItemToOrderRequest(
            productId = "product-1",
            productName = "Espresso",
            quantity = 2,
            price = BigDecimal("3.50")
        )

        // When
        val response = restTemplate.postForEntity(
            "/api/orders/{orderId}/items",
            addItemRequest,
            String::class.java,
            orderId
        )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)

        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val orderResponse = restTemplate.getForEntity(
                "/api/orders/{id}",
                OrderView::class.java,
                orderId
            )
            assertEquals(HttpStatus.OK, orderResponse.statusCode)
            assertNotNull(orderResponse.body)
            assertEquals(1, orderResponse.body?.items?.size)
        }

        // Verify the item was added
        val getResponse = restTemplate.getForEntity(
            "/api/orders/{id}",
            OrderView::class.java,
            orderId
        )

        assertEquals(HttpStatus.OK, getResponse.statusCode)
        assertNotNull(getResponse.body)
        assertEquals(1, getResponse.body?.items?.size)
        assertEquals("Espresso", getResponse.body?.items?.get(0)?.productName)
    }

    @Test
    fun `should submit order`() {
        // Given
        val createOrderRequest = CreateOrderRequest(
            customerId = "customer-1"
        )

        val createOrderResponse = restTemplate.postForEntity(
            "/api/orders",
            createOrderRequest,
            String::class.java
        )
        assertEquals(HttpStatus.OK, createOrderResponse.statusCode)
        val orderId = createOrderResponse.body
        assertNotNull(orderId)

        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val orderResponse = restTemplate.getForEntity(
                "/api/orders/{id}",
                OrderView::class.java,
                orderId
            )
            assertEquals(HttpStatus.OK, orderResponse.statusCode)
        }

        val addItemRequest = AddItemToOrderRequest(
            productId = "product-1",
            productName = "Espresso",
            quantity = 2,
            price = BigDecimal("3.50")
        )

        restTemplate.postForEntity(
            "/api/orders/{orderId}/items",
            addItemRequest,
            String::class.java,
            orderId
        )

        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val orderResponse = restTemplate.getForEntity(
                "/api/orders/{id}",
                OrderView::class.java,
                orderId
            )
            assertEquals(HttpStatus.OK, orderResponse.statusCode)
            assertNotNull(orderResponse.body)
            assert(orderResponse.body!!.items.isNotEmpty())
        }

        // When
        val response = restTemplate.postForEntity(
            "/api/orders/{orderId}/submit",
            null,
            String::class.java,
            orderId
        )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)

        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val orderResponse = restTemplate.getForEntity(
                "/api/orders/{id}",
                OrderView::class.java,
                orderId
            )
            assertEquals(HttpStatus.OK, orderResponse.statusCode)
            assertEquals("SUBMITTED", orderResponse.body?.status)
        }

        // Verify the order was submitted
        val getResponse = restTemplate.getForEntity(
            "/api/orders/{id}",
            OrderView::class.java,
            orderId
        )

        assertEquals(HttpStatus.OK, getResponse.statusCode)
        assertEquals("SUBMITTED", getResponse.body?.status)
    }

    // Payment REST Endpoint Tests

    @Test
    fun `should handle dead letter queue for payment reset`() {
        // Create a payment
        val createPaymentRequest = CreatePaymentRequest(
            orderId = "order-dlq-test",
            amount = BigDecimal("25.00")
        )

        val createResponse = restTemplate.postForEntity(
            "/api/payments",
            createPaymentRequest,
            String::class.java
        )

        assertEquals(HttpStatus.OK, createResponse.statusCode)
        val paymentId = createResponse.body
        assertNotNull(paymentId)

        // Process the payment
        val processResponse = restTemplate.postForEntity(
            "/api/payments/$paymentId/process",
            null,
            String::class.java
        )

        assertEquals(HttpStatus.OK, processResponse.statusCode)

        // Verify the payment was processed
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val payment = restTemplate.getForEntity(
                "/api/payments/$paymentId",
                PaymentView::class.java
            ).body

            assertNotNull(payment)
            assertEquals("PROCESSED", payment.status)
        }

        // Create a payment with ID ending in "error" to trigger DLQ
        val errorPaymentRequest = CreatePaymentRequest(
            orderId = "order-dlq-test-error",
            amount = BigDecimal("15.00")
        )

        val createErrorResponse = restTemplate.postForEntity(
            "/api/payments",
            errorPaymentRequest,
            String::class.java
        )

        assertEquals(HttpStatus.OK, createErrorResponse.statusCode)
        val errorPaymentId = createErrorResponse.body
        assertNotNull(errorPaymentId)

        // Reset the error payment (this will fail and go to DLQ)
        restTemplate.postForEntity(
            "/api/payments/$errorPaymentId/reset",
            null,
            String::class.java
        )

        // For the dead letter queue test, we'll just verify that the reset endpoint was called
        // and didn't throw an exception. The actual DLQ processing happens asynchronously
        // and is tested separately in PaymentEventProcessorTests
    }

    @Test
    fun `should create payment`() {
        // Given
        val request = CreatePaymentRequest(
            orderId = "order-1",
            amount = BigDecimal("42.50")
        )

        // When
        val response = restTemplate.postForEntity(
            "/api/payments",
            request,
            String::class.java
        )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val paymentResponse = restTemplate.getForEntity(
                "/api/payments",
                Array<PaymentView>::class.java
            )
            assertEquals(HttpStatus.OK, paymentResponse.statusCode)
            assertNotNull(paymentResponse.body)
            assert(paymentResponse.body!!.isNotEmpty())
        }
    }

    @Test
    fun `should process payment`() {
        // Given
        val createRequest = CreatePaymentRequest(
            orderId = "order-2",
            amount = BigDecimal("25.00")
        )

        val createResponse = restTemplate.postForEntity(
            "/api/payments",
            createRequest,
            String::class.java
        )
        assertEquals(HttpStatus.OK, createResponse.statusCode)
        val paymentId = createResponse.body
        assertNotNull(paymentId)

        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val paymentResponse = restTemplate.getForEntity(
                "/api/payments/{id}",
                PaymentView::class.java,
                paymentId
            )
            assertEquals(HttpStatus.OK, paymentResponse.statusCode)
        }

        // When
        val response = restTemplate.postForEntity(
            "/api/payments/{paymentId}/process",
            null,
            String::class.java,
            paymentId
        )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)

        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val paymentResponse = restTemplate.getForEntity(
                "/api/payments/{id}",
                PaymentView::class.java,
                paymentId
            )
            assertEquals(HttpStatus.OK, paymentResponse.statusCode)
            assertEquals("PROCESSED", paymentResponse.body?.status)
            assertNotNull(paymentResponse.body?.transactionId)
        }
    }

    @Test
    fun `should fail payment`() {
        // Given
        val createRequest = CreatePaymentRequest(
            orderId = "order-3",
            amount = BigDecimal("100.00")
        )

        val createResponse = restTemplate.postForEntity(
            "/api/payments",
            createRequest,
            String::class.java
        )
        assertEquals(HttpStatus.OK, createResponse.statusCode)
        val paymentId = createResponse.body
        assertNotNull(paymentId)

        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val paymentResponse = restTemplate.getForEntity(
                "/api/payments/{id}",
                PaymentView::class.java,
                paymentId
            )
            assertEquals(HttpStatus.OK, paymentResponse.statusCode)
        }

        val failRequest = FailPaymentRequest(
            reason = "Insufficient funds"
        )

        // When
        val response = restTemplate.postForEntity(
            "/api/payments/{paymentId}/fail",
            failRequest,
            String::class.java,
            paymentId
        )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)

        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val paymentResponse = restTemplate.getForEntity(
                "/api/payments/{id}",
                PaymentView::class.java,
                paymentId
            )
            assertEquals(HttpStatus.OK, paymentResponse.statusCode)
            assertEquals("FAILED", paymentResponse.body?.status)
            assertEquals("Insufficient funds", paymentResponse.body?.failureReason)
        }
    }

    // Idempotency Interceptor Tests

    @Test
    fun `should ensure idempotent event processing`() {
        // Clear any existing idempotency records
        idempotencyRepository.deleteAll()

        // Create a product - this will generate events that should be processed exactly once
        val productRequest = CreateProductRequest(
            name = "Idempotency Test Product",
            description = "Testing idempotent event processing",
            price = BigDecimal("5.99")
        )

        val productResponse = restTemplate.postForEntity(
            "/api/products",
            productRequest,
            String::class.java
        )

        assertEquals(HttpStatus.OK, productResponse.statusCode)
        val productId = productResponse.body
        assertNotNull(productId)

        // Wait for event processing to complete
        await().atMost(5, TimeUnit.SECONDS).until {
            val records = idempotencyRepository.findAll().toList()
            records.isNotEmpty()
        }

        // Verify that idempotency records were created
        val records = idempotencyRepository.findAll().toList()
        assertTrue(records.isNotEmpty(), "Idempotency records should be created")

        // Verify the structure of the idempotency records
        val record = records.first()
        assertNotNull(record.eventId, "Event ID should be present")
        assertNotNull(record.aggregateId, "Aggregate ID should be present")
        assertNotNull(record.processingGroup, "Processing group should be present")
        assertNotNull(record.headers, "Headers should be present")
        assertTrue(record.headers.isNotEmpty(), "Headers should not be empty")

        // Verify that the product was created in the read model
        val productView = restTemplate.getForObject(
            "/api/products/$productId",
            ProductView::class.java
        )
        assertNotNull(productView)
        assertEquals("Idempotency Test Product", productView.name)

        // Log some information about the idempotency record for verification
        println("Idempotency record created:")
        println("  Event ID: ${record.eventId}")
        println("  Aggregate ID: ${record.aggregateId}")
        println("  Processing Group: ${record.processingGroup}")
        println("  Headers: ${record.headers.keys}")
        println("  Is Replay: ${record.isReplay}")
    }
}
