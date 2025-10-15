package wtf.milehimikey.coffeeshop.products

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SkuLookupServiceTest {

    private lateinit var skuLookupService: SkuLookupService

    @BeforeEach
    fun setUp() {
        skuLookupService = SkuLookupService()
        // Don't call loadMappings() - we'll manually add mappings for testing
    }

    @Test
    fun `should return SKU from CSV mapping when available`() {
        // Given
        skuLookupService.addMapping("product-1", "ESP-001")

        // When
        val sku = skuLookupService.getSkuForProduct("product-1", "Espresso")

        // Then
        assertEquals("ESP-001", sku)
    }

    @Test
    fun `should generate name-based SKU when CSV mapping not available`() {
        // Given - no mapping added

        // When
        val sku = skuLookupService.getSkuForProduct("product-unknown", "Espresso")

        // Then
        assertEquals("ESP-LEGACY", sku)
    }

    @Test
    fun `should generate name-based SKU with first 3 characters uppercase`() {
        // Test various product names
        assertEquals("LAT-LEGACY", skuLookupService.getSkuForProduct("product-2", "Latte"))
        assertEquals("CAP-LEGACY", skuLookupService.getSkuForProduct("product-3", "Cappuccino"))
        assertEquals("MOC-LEGACY", skuLookupService.getSkuForProduct("product-4", "Mocha"))
    }

    @Test
    fun `should generate ID-based SKU when product name is null`() {
        // Given - no mapping and no product name

        // When
        val sku = skuLookupService.getSkuForProduct("product-unknown", null)

        // Then
        assertEquals("PROD-product-unknown", sku)
    }

    @Test
    fun `should generate ID-based SKU when product name is empty`() {
        // Given - no mapping and empty product name

        // When
        val sku = skuLookupService.getSkuForProduct("product-unknown", "")

        // Then
        assertEquals("PROD-product-unknown", sku)
    }

    @Test
    fun `should handle product names with special characters`() {
        // Given - product name with special characters

        // When
        val sku = skuLookupService.getSkuForProduct("product-5", "Café Latte")

        // Then
        // Should extract first 3 alphanumeric characters
        assertEquals("CAF-LEGACY", sku)
    }

    @Test
    fun `should handle short product names`() {
        // Given - product name with less than 3 characters

        // When
        val sku = skuLookupService.getSkuForProduct("product-6", "Tea")

        // Then
        assertEquals("TEA-LEGACY", sku)
    }

    @Test
    fun `should handle product names with only special characters`() {
        // Given - product name with only special characters

        // When
        val sku = skuLookupService.getSkuForProduct("product-7", "!!!")

        // Then
        // Should fall back to UNK-LEGACY when no alphanumeric characters
        assertEquals("UNK-LEGACY", sku)
    }

    @Test
    fun `should prefer CSV mapping over name-based generation`() {
        // Given
        skuLookupService.addMapping("product-1", "CUSTOM-SKU")

        // When
        val sku = skuLookupService.getSkuForProduct("product-1", "Espresso")

        // Then
        assertEquals("CUSTOM-SKU", sku)
    }

    @Test
    fun `should return all loaded mappings`() {
        // Given
        skuLookupService.addMapping("product-1", "ESP-001")
        skuLookupService.addMapping("product-2", "LAT-001")

        // When
        val mappings = skuLookupService.getAllMappings()

        // Then
        assertEquals(2, mappings.size)
        assertEquals("ESP-001", mappings["product-1"])
        assertEquals("LAT-001", mappings["product-2"])
    }

    @Test
    fun `should handle multiple products with same name prefix`() {
        // Given - multiple products with names starting with same letters

        // When
        val sku1 = skuLookupService.getSkuForProduct("product-1", "Espresso")
        val sku2 = skuLookupService.getSkuForProduct("product-2", "Espresso Macchiato")

        // Then
        // Both should get same name-based SKU (deterministic)
        assertEquals("ESP-LEGACY", sku1)
        assertEquals("ESP-LEGACY", sku2)
    }

    @Test
    fun `should be deterministic - same input produces same output`() {
        // Given
        val productId = "product-test"
        val productName = "Test Product"

        // When - call multiple times
        val sku1 = skuLookupService.getSkuForProduct(productId, productName)
        val sku2 = skuLookupService.getSkuForProduct(productId, productName)
        val sku3 = skuLookupService.getSkuForProduct(productId, productName)

        // Then - all should be identical
        assertEquals(sku1, sku2)
        assertEquals(sku2, sku3)
        assertEquals("TES-LEGACY", sku1)
    }

    @Test
    fun `should handle lowercase product names`() {
        // Given - lowercase product name

        // When
        val sku = skuLookupService.getSkuForProduct("product-8", "espresso")

        // Then
        assertEquals("ESP-LEGACY", sku)
    }

    @Test
    fun `should handle mixed case product names`() {
        // Given - mixed case product name

        // When
        val sku = skuLookupService.getSkuForProduct("product-9", "EsPrEsSo")

        // Then
        assertEquals("ESP-LEGACY", sku)
    }
}

