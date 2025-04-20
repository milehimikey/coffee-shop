package wtf.milehimikey.coffeeshop

import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.messaging.responsetypes.ResponseTypes
import org.axonframework.queryhandling.QueryGateway
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import wtf.milehimikey.coffeeshop.products.*
import java.math.BigDecimal
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/api")
class RestEndpoint(
    private val commandGateway: CommandGateway,
    private val queryGateway: QueryGateway
) {

    // Product Endpoints

    @PostMapping("/products")
    fun createProduct(@RequestBody request: CreateProductRequest): CompletableFuture<ResponseEntity<String>> {
        val command = CreateProduct(
            name = request.name,
            description = request.description,
            price = request.price
        )
        return commandGateway.send<String>(command)
            .thenApply { productId -> ResponseEntity.ok(productId) }
            .exceptionally { e -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message) }
    }

    @PutMapping("/products/{id}")
    fun updateProduct(
        @PathVariable id: String,
        @RequestBody request: UpdateProductRequest
    ): CompletableFuture<ResponseEntity<String>> {
        val command = UpdateProduct(
            id = id,
            name = request.name,
            description = request.description,
            price = request.price
        )
        return commandGateway.send<String>(command)
            .thenApply { ResponseEntity.ok(id) }
            .exceptionally { e -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message) }
    }

    @DeleteMapping("/products/{id}")
    fun deleteProduct(@PathVariable id: String): CompletableFuture<ResponseEntity<String>> {
        val command = DeleteProduct(id = id)
        return commandGateway.send<String>(command)
            .thenApply { ResponseEntity.ok(id) }
            .exceptionally { e -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message) }
    }

    @GetMapping("/products/{id}")
    fun getProduct(@PathVariable id: String): CompletableFuture<ResponseEntity<ProductView>> {
        return queryGateway.query(
            FindProductById(id),
            ResponseTypes.instanceOf(ProductView::class.java)
        ).thenApply { product ->
            if (product != null) {
                ResponseEntity.ok(product)
            } else {
                ResponseEntity.notFound().build()
            }
        }
    }

    @GetMapping("/products")
    fun getAllProducts(
        @RequestParam(required = false, defaultValue = "false") includeInactive: Boolean
    ): CompletableFuture<List<ProductView>> {
        return queryGateway.query(
            FindAllProducts(includeInactive),
            ResponseTypes.multipleInstancesOf(ProductView::class.java)
        )
    }
}

// Request DTOs
data class CreateProductRequest(
    val name: String,
    val description: String,
    val price: BigDecimal
)

data class UpdateProductRequest(
    val name: String,
    val description: String,
    val price: BigDecimal
)
