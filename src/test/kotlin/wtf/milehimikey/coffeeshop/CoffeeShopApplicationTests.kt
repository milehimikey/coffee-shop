package wtf.milehimikey.coffeeshop

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import wtf.milehimikey.coffeeshop.products.ProductView
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Import(TestcontainersConfiguration::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CoffeeShopApplicationTests {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

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

        // Wait a bit for event processing
        Thread.sleep(500)
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

        // Wait a bit for event processing
        Thread.sleep(500)

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

        // Wait a bit for event processing
        Thread.sleep(500)

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

        // Wait a bit for event processing
        Thread.sleep(500)

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

        // Wait a bit for event processing
        Thread.sleep(500)

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

        // Wait a bit for event processing
        Thread.sleep(1000)

        // When
        val response = restTemplate.getForEntity(
            "/api/products",
            Array<ProductView>::class.java
        )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(3, response.body?.size)
    }
}
