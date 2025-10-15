package wtf.milehimikey.coffeeshop.products

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Service that provides SKU lookup for products using a multi-strategy fallback approach.
 * 
 * Strategy chain:
 * 1. CSV Lookup - Primary source from product team (sku-mappings.csv)
 * 2. Name-based Generation - Generate SKU from product name (e.g., "Espresso" -> "ESP-LEGACY")
 * 3. ID-based Generation - Fallback using product ID (e.g., "PROD-{id}")
 * 
 * This service is used by the ProductCreatedUpcaster to add SKU fields to historical events
 * that were created before the SKU field was required.
 */
@Service
class SkuLookupService {
    
    private val logger = LoggerFactory.getLogger(SkuLookupService::class.java)
    private val mappings = ConcurrentHashMap<String, String>()
    
    @PostConstruct
    fun loadMappings() {
        logger.info("Loading SKU mappings...")
        
        // Try multiple sources in order of preference
        var loaded = loadFromClasspath()
        
        if (!loaded) {
            loaded = loadFromExternalFile()
        }
        
        logger.info("Loaded ${mappings.size} SKU mappings")
    }
    
    /**
     * Get SKU for a product using the fallback strategy chain.
     * 
     * @param productId The product ID
     * @param productName The product name (optional, used for name-based fallback)
     * @return The SKU for the product (never null)
     */
    fun getSkuForProduct(productId: String, productName: String? = null): String {
        // Strategy 1: CSV Lookup
        mappings[productId]?.let {
            logger.debug("Found SKU for product $productId in CSV: $it")
            return it
        }

        // Strategy 2: Name-based Generation
        if (!productName.isNullOrBlank()) {
            val nameBased = generateSkuFromName(productName)
            logger.debug("Generated name-based SKU for product $productId: $nameBased")
            return nameBased
        }

        // Strategy 3: ID-based Generation (always succeeds)
        val idBased = generateSkuFromId(productId)
        logger.debug("Generated ID-based SKU for product $productId: $idBased")
        return idBased
    }
    
    /**
     * Load SKU mappings from classpath resource (src/main/resources/sku-mappings.csv).
     * 
     * @return true if file was found and loaded, false otherwise
     */
    private fun loadFromClasspath(): Boolean {
        return try {
            val resource = ClassPathResource("sku-mappings.csv")
            if (resource.exists()) {
                logger.info("Loading SKU mappings from classpath: sku-mappings.csv")
                parseCsvIntoMappings(resource.file)
                true
            } else {
                logger.warn("No classpath SKU mapping found at sku-mappings.csv")
                false
            }
        } catch (e: Exception) {
            logger.warn("Failed to load SKU mappings from classpath: ${e.message}")
            false
        }
    }
    
    /**
     * Load SKU mappings from external file (/etc/coffee-shop/sku-mappings.csv).
     * This allows updating mappings without redeploying the application.
     * 
     * @return true if file was found and loaded, false otherwise
     */
    private fun loadFromExternalFile(): Boolean {
        return try {
            val externalFile = File("/etc/coffee-shop/sku-mappings.csv")
            if (externalFile.exists()) {
                logger.info("Loading SKU mappings from external file: ${externalFile.absolutePath}")
                parseCsvIntoMappings(externalFile)
                true
            } else {
                logger.info("No external SKU mapping file found at ${externalFile.absolutePath}")
                false
            }
        } catch (e: Exception) {
            logger.warn("Failed to load SKU mappings from external file: ${e.message}")
            false
        }
    }
    
    /**
     * Parse CSV file and load mappings into memory.
     * 
     * Expected CSV format:
     * ```
     * product_id,sku
     * product-1,ESP-001
     * product-2,LAT-001
     * ```
     * 
     * @param file The CSV file to parse
     */
    private fun parseCsvIntoMappings(file: File) {
        var lineNumber = 0
        var loadedCount = 0
        
        file.readLines().forEach { line ->
            lineNumber++
            
            // Skip header line
            if (lineNumber == 1) {
                return@forEach
            }
            
            // Skip empty lines
            if (line.isBlank()) {
                return@forEach
            }
            
            try {
                val parts = line.split(",")
                if (parts.size >= 2) {
                    val productId = parts[0].trim()
                    val sku = parts[1].trim()
                    
                    if (productId.isNotEmpty() && sku.isNotEmpty()) {
                        mappings[productId] = sku
                        loadedCount++
                    } else {
                        logger.warn("Skipping line $lineNumber: empty product_id or sku")
                    }
                } else {
                    logger.warn("Skipping line $lineNumber: invalid format (expected: product_id,sku)")
                }
            } catch (e: Exception) {
                logger.warn("Error parsing line $lineNumber: ${e.message}")
            }
        }
        
        logger.info("Loaded $loadedCount SKU mappings from CSV")
    }
    
    /**
     * Generate SKU from product name.
     * 
     * Strategy: Take first 3 characters of name (uppercase) + "-LEGACY"
     * Examples:
     * - "Espresso" -> "ESP-LEGACY"
     * - "Latte" -> "LAT-LEGACY"
     * - "Cappuccino" -> "CAP-LEGACY"
     * 
     * @param name The product name
     * @return Generated SKU
     */
    private fun generateSkuFromName(name: String): String {
        val prefix = name.take(3).uppercase().replace(Regex("[^A-Z0-9]"), "")
        return if (prefix.isNotEmpty()) {
            "$prefix-LEGACY"
        } else {
            "UNK-LEGACY"
        }
    }
    
    /**
     * Generate SKU from product ID.
     * 
     * This is the final fallback strategy that always succeeds.
     * 
     * Strategy: "PROD-" + product ID
     * Example: "product-1" -> "PROD-product-1"
     * 
     * @param productId The product ID
     * @return Generated SKU
     */
    private fun generateSkuFromId(productId: String): String {
        return "PROD-$productId"
    }
    
    /**
     * Get all loaded mappings (for testing/debugging).
     * 
     * @return Map of product ID to SKU
     */
    fun getAllMappings(): Map<String, String> {
        return mappings.toMap()
    }
    
    /**
     * Manually add a SKU mapping (useful for testing or runtime updates).
     * 
     * @param productId The product ID
     * @param sku The SKU
     */
    fun addMapping(productId: String, sku: String) {
        mappings[productId] = sku
        logger.info("Added SKU mapping: $productId -> $sku")
    }
}

