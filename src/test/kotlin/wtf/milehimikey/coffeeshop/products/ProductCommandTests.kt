package wtf.milehimikey.coffeeshop.products

import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ProductCommandTests {
    
    private lateinit var fixture: FixtureConfiguration<Product>
    
    @BeforeEach
    fun setUp() {
        fixture = AggregateTestFixture(Product::class.java)
    }
    
    @Test
    fun `should create product`() {
        val command = CreateProduct(
            id = "product-1",
            name = "Espresso",
            description = "Strong coffee",
            price = BigDecimal("3.50")
        )
        
        val expectedEvent = ProductCreated(
            id = "product-1",
            name = "Espresso",
            description = "Strong coffee",
            price = BigDecimal("3.50")
        )
        
        fixture.givenNoPriorActivity()
            .`when`(command)
            .expectSuccessfulHandlerExecution()
            .expectEvents(expectedEvent)
    }
    
    @Test
    fun `should update product`() {
        val id = "product-1"
        val createCommand = CreateProduct(
            id = id,
            name = "Espresso",
            description = "Strong coffee",
            price = BigDecimal("3.50")
        )
        
        val updateCommand = UpdateProduct(
            id = id,
            name = "Double Espresso",
            description = "Extra strong coffee",
            price = BigDecimal("4.50")
        )
        
        val expectedEvent = ProductUpdated(
            id = id,
            name = "Double Espresso",
            description = "Extra strong coffee",
            price = BigDecimal("4.50")
        )
        
        fixture.given(
            ProductCreated(
                id = id,
                name = "Espresso",
                description = "Strong coffee",
                price = BigDecimal("3.50")
            )
        )
            .`when`(updateCommand)
            .expectSuccessfulHandlerExecution()
            .expectEvents(expectedEvent)
    }
    
    @Test
    fun `should delete product`() {
        val id = "product-1"
        val deleteCommand = DeleteProduct(id = id)
        
        val expectedEvent = ProductDeleted(id = id)
        
        fixture.given(
            ProductCreated(
                id = id,
                name = "Espresso",
                description = "Strong coffee",
                price = BigDecimal("3.50")
            )
        )
            .`when`(deleteCommand)
            .expectSuccessfulHandlerExecution()
            .expectEvents(expectedEvent)
    }
    
    @Test
    fun `should not update deleted product`() {
        val id = "product-1"
        val updateCommand = UpdateProduct(
            id = id,
            name = "Double Espresso",
            description = "Extra strong coffee",
            price = BigDecimal("4.50")
        )
        
        fixture.given(
            ProductCreated(
                id = id,
                name = "Espresso",
                description = "Strong coffee",
                price = BigDecimal("3.50")
            ),
            ProductDeleted(id = id)
        )
            .`when`(updateCommand)
            .expectException(IllegalStateException::class.java)
    }
    
    @Test
    fun `should not delete already deleted product`() {
        val id = "product-1"
        val deleteCommand = DeleteProduct(id = id)
        
        fixture.given(
            ProductCreated(
                id = id,
                name = "Espresso",
                description = "Strong coffee",
                price = BigDecimal("3.50")
            ),
            ProductDeleted(id = id)
        )
            .`when`(deleteCommand)
            .expectException(IllegalStateException::class.java)
    }
}
