package wtf.milehimikey.coffeeshop

import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.messaging.responsetypes.ResponseTypes
import org.axonframework.queryhandling.QueryGateway
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import wtf.milehimikey.coffeeshop.orders.*
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

    // Order Endpoints

    @PostMapping("/orders")
    fun createOrder(@RequestBody request: CreateOrderRequest): CompletableFuture<ResponseEntity<String>> {
        val command = CreateOrder(
            customerId = request.customerId
        )
        return commandGateway.send<String>(command)
            .thenApply { orderId -> ResponseEntity.ok(orderId) }
            .exceptionally { e -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message) }
    }

    @PostMapping("/orders/{orderId}/items")
    fun addItemToOrder(
        @PathVariable orderId: String,
        @RequestBody request: AddItemToOrderRequest
    ): CompletableFuture<ResponseEntity<String>> {
        val command = AddItemToOrder(
            orderId = orderId,
            productId = request.productId,
            productName = request.productName,
            quantity = request.quantity,
            price = request.price
        )
        return commandGateway.send<String>(command)
            .thenApply { ResponseEntity.ok(orderId) }
            .exceptionally { e -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message) }
    }

    @PostMapping("/orders/{orderId}/submit")
    fun submitOrder(@PathVariable orderId: String): CompletableFuture<ResponseEntity<String>> {
        val command = SubmitOrder(orderId = orderId)
        return commandGateway.send<String>(command)
            .thenApply { ResponseEntity.ok(orderId) }
            .exceptionally { e -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message) }
    }

    @PostMapping("/orders/{orderId}/deliver")
    fun deliverOrder(@PathVariable orderId: String): CompletableFuture<ResponseEntity<String>> {
        val command = DeliverOrder(orderId = orderId)
        return commandGateway.send<String>(command)
            .thenApply { ResponseEntity.ok(orderId) }
            .exceptionally { e -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message) }
    }

    @PostMapping("/orders/{orderId}/complete")
    fun completeOrder(@PathVariable orderId: String): CompletableFuture<ResponseEntity<String>> {
        val command = CompleteOrder(orderId = orderId)
        return commandGateway.send<String>(command)
            .thenApply { ResponseEntity.ok(orderId) }
            .exceptionally { e -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message) }
    }

    @GetMapping("/orders/{id}")
    fun getOrder(@PathVariable id: String): CompletableFuture<ResponseEntity<OrderView>> {
        return queryGateway.query(
            FindOrderById(id),
            ResponseTypes.instanceOf(OrderView::class.java)
        ).thenApply { order ->
            if (order != null) {
                ResponseEntity.ok(order)
            } else {
                ResponseEntity.notFound().build()
            }
        }
    }

    @GetMapping("/orders")
    fun getAllOrders(
        @RequestParam(required = false) customerId: String?,
        @RequestParam(required = false) status: String?
    ): CompletableFuture<List<OrderView>> {
        return when {
            customerId != null -> queryGateway.query(
                FindOrdersByCustomerId(customerId),
                ResponseTypes.multipleInstancesOf(OrderView::class.java)
            )
            status != null -> queryGateway.query(
                FindOrdersByStatus(status),
                ResponseTypes.multipleInstancesOf(OrderView::class.java)
            )
            else -> queryGateway.query(
                FindAllOrders(),
                ResponseTypes.multipleInstancesOf(OrderView::class.java)
            )
        }
    }
}

// Product Request DTOs
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

// Order Request DTOs
data class CreateOrderRequest(
    val customerId: String
)

data class AddItemToOrderRequest(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val price: BigDecimal
)
