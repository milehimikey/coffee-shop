package wtf.milehimikey.coffeeshop

import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.messaging.responsetypes.ResponseTypes
import org.axonframework.queryhandling.QueryGateway
import org.javamoney.moneta.Money
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import wtf.milehimikey.coffeeshop.admin.BatchGenerationResult
import wtf.milehimikey.coffeeshop.admin.DataGenerator

import wtf.milehimikey.coffeeshop.orders.AddItemToOrder
import wtf.milehimikey.coffeeshop.orders.CompleteOrder
import wtf.milehimikey.coffeeshop.orders.CorrectOrderItemProductName
import wtf.milehimikey.coffeeshop.orders.CreateOrder
import wtf.milehimikey.coffeeshop.orders.DeliverOrder
import wtf.milehimikey.coffeeshop.orders.FindAllOrders
import wtf.milehimikey.coffeeshop.orders.FindOrderById
import wtf.milehimikey.coffeeshop.orders.FindOrdersByCustomerId
import wtf.milehimikey.coffeeshop.orders.FindOrdersByStatus
import wtf.milehimikey.coffeeshop.orders.OrderView
import wtf.milehimikey.coffeeshop.orders.SubmitOrder
import wtf.milehimikey.coffeeshop.payments.CreatePayment
import wtf.milehimikey.coffeeshop.payments.FailPayment
import wtf.milehimikey.coffeeshop.payments.FindAllPayments
import wtf.milehimikey.coffeeshop.payments.FindPaymentById
import wtf.milehimikey.coffeeshop.payments.FindPaymentsByOrderId
import wtf.milehimikey.coffeeshop.payments.FindPaymentsByStatus
import wtf.milehimikey.coffeeshop.payments.PaymentView
import wtf.milehimikey.coffeeshop.payments.ProcessPayment
import wtf.milehimikey.coffeeshop.payments.RefundPayment
import wtf.milehimikey.coffeeshop.payments.ResetPayment
import wtf.milehimikey.coffeeshop.products.CreateProduct
import wtf.milehimikey.coffeeshop.products.DeleteProduct
import wtf.milehimikey.coffeeshop.products.FindAllProducts
import wtf.milehimikey.coffeeshop.products.FindProductById
import wtf.milehimikey.coffeeshop.products.ProductView
import wtf.milehimikey.coffeeshop.products.UpdateProduct
import java.math.BigDecimal
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/api")
class RestEndpoint(
    private val commandGateway: CommandGateway,
    private val queryGateway: QueryGateway,
    private val dataGenerator: DataGenerator
) {

    // Product Endpoints

    @PostMapping("/products")
    fun createProduct(@RequestBody request: CreateProductRequest): CompletableFuture<ResponseEntity<String>> {
        val command = CreateProduct(
            name = request.name,
            description = request.description,
            price = Money.of(request.price, "USD"),
            sku = request.sku
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
            price = Money.of(request.price, "USD")
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
            price = Money.of(request.price, "USD")
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

    // Admin Endpoints for Order Corrections

    /**
     * Admin endpoint to correct a product name for an order item.
     * This demonstrates the event sourcing pattern of using compensating events
     * to fix data quality issues without modifying historical events.
     *
     * Example usage:
     * POST /api/admin/orders/{orderId}/items/{productId}/correct-name
     * Body: {"correctedProductName": "Espresso"}
     */
    @PostMapping("/admin/orders/{orderId}/items/{productId}/correct-name")
    fun correctProductName(
        @PathVariable orderId: String,
        @PathVariable productId: String,
        @RequestBody request: CorrectProductNameRequest
    ): CompletableFuture<ResponseEntity<String>> {
        val command = CorrectOrderItemProductName(
            orderId = orderId,
            productId = productId,
            correctedProductName = request.correctedProductName
        )
        return commandGateway.send<String>(command)
            .thenApply { ResponseEntity.ok("Product name corrected successfully for order $orderId, product $productId") }
            .exceptionally { e -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: ${e.message}") }
    }

    // Payment Endpoints

    @PostMapping("/payments")
    fun createPayment(@RequestBody request: CreatePaymentRequest): CompletableFuture<ResponseEntity<String>> {
        val command = CreatePayment(
            orderId = request.orderId,
            amount = request.amount
        )
        return commandGateway.send<String>(command)
            .thenApply { paymentId -> ResponseEntity.ok(paymentId) }
            .exceptionally { e -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message) }
    }

    @PostMapping("/payments/{paymentId}/process")
    fun processPayment(@PathVariable paymentId: String): CompletableFuture<ResponseEntity<String>> {
        val command = ProcessPayment(paymentId = paymentId)
        return commandGateway.send<String>(command)
            .thenApply { ResponseEntity.ok(paymentId) }
            .exceptionally { e -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message) }
    }

    @PostMapping("/payments/{paymentId}/fail")
    fun failPayment(
        @PathVariable paymentId: String,
        @RequestBody request: FailPaymentRequest
    ): CompletableFuture<ResponseEntity<String>> {
        val command = FailPayment(paymentId = paymentId, reason = request.reason)
        return commandGateway.send<String>(command)
            .thenApply { ResponseEntity.ok(paymentId) }
            .exceptionally { e -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message) }
    }

    @PostMapping("/payments/{paymentId}/refund")
    fun refundPayment(@PathVariable paymentId: String): CompletableFuture<ResponseEntity<String>> {
        val command = RefundPayment(paymentId = paymentId)
        return commandGateway.send<String>(command)
            .thenApply { ResponseEntity.ok(paymentId) }
            .exceptionally { e -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message) }
    }

    /**
     * Reset a payment to PENDING status.
     * This endpoint is primarily for testing the dead letter queue functionality.
     * If the payment ID ends with "error", the event processor will throw an exception
     * and the event will be stored in the dead letter queue.
     */
    @PostMapping("/payments/{paymentId}/reset")
    fun resetPayment(@PathVariable paymentId: String): CompletableFuture<ResponseEntity<String>> {
        val command = ResetPayment(paymentId = paymentId)
        return commandGateway.send<String>(command)
            .thenApply { ResponseEntity.ok(paymentId) }
            .exceptionally { e -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message) }
    }

    @GetMapping("/payments/{id}")
    fun getPayment(@PathVariable id: String): CompletableFuture<ResponseEntity<PaymentView>> {
        return queryGateway.query(
            FindPaymentById(id),
            ResponseTypes.instanceOf(PaymentView::class.java)
        ).thenApply { payment ->
            if (payment != null) {
                ResponseEntity.ok(payment)
            } else {
                ResponseEntity.notFound().build()
            }
        }
    }

    @GetMapping("/payments")
    fun getAllPayments(
        @RequestParam(required = false) orderId: String?,
        @RequestParam(required = false) status: String?
    ): CompletableFuture<List<PaymentView>> {
        return when {
            orderId != null -> queryGateway.query(
                FindPaymentsByOrderId(orderId),
                ResponseTypes.multipleInstancesOf(PaymentView::class.java)
            )
            status != null -> queryGateway.query(
                FindPaymentsByStatus(status),
                ResponseTypes.multipleInstancesOf(PaymentView::class.java)
            )
            else -> queryGateway.query(
                FindAllPayments(),
                ResponseTypes.multipleInstancesOf(PaymentView::class.java)
            )
        }
    }

    // Data Generation Endpoints

    @PostMapping("/generate/products")
    fun generateProducts(@RequestBody request: GenerateProductsRequest): ResponseEntity<List<String>> {
        val productIds = dataGenerator.generateProducts(
            count = request.count,
            triggerSnapshot = request.triggerSnapshot
        )
        return ResponseEntity.ok(productIds)
    }

    @PostMapping("/generate/orders")
    fun generateOrders(@RequestBody request: GenerateOrdersRequest): ResponseEntity<List<String>> {
        val orderIds = dataGenerator.generateOrders(
            count = request.count,
            customerId = request.customerId,
            itemsPerOrder = request.itemsPerOrder,
            triggerSnapshot = request.triggerSnapshot,
            completeOrders = request.completeOrders
        )
        return ResponseEntity.ok(orderIds)
    }

    @PostMapping("/generate/payments")
    fun generatePayments(@RequestBody request: GeneratePaymentsRequest): ResponseEntity<List<String>> {
        val paymentIds = dataGenerator.generatePayments(
            orderIds = request.orderIds,
            triggerSnapshot = request.triggerSnapshot,
            triggerDeadLetter = request.triggerDeadLetter
        )
        return ResponseEntity.ok(paymentIds)
    }

    @PostMapping("/generate/batch")
    fun generateBatch(@RequestBody request: GenerateBatchRequest): ResponseEntity<BatchGenerationResult> {
        val result = dataGenerator.generateBatch(
            productCount = request.productCount,
            orderCount = request.orderCount,
            triggerSnapshots = request.triggerSnapshots,
            triggerDeadLetters = request.triggerDeadLetters
        )
        return ResponseEntity.ok(result)
    }


}

// Product Request DTOs
data class CreateProductRequest(
    val name: String,
    val description: String,
    val price: BigDecimal,
    val sku: String
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

data class CorrectProductNameRequest(
    val correctedProductName: String
)

// Payment Request DTOs
data class CreatePaymentRequest(
    val orderId: String,
    val amount: BigDecimal
)

data class FailPaymentRequest(
    val reason: String
)

// Data Generation Request DTOs
data class GenerateProductsRequest(
    val count: Int = 10,
    val triggerSnapshot: Boolean = false
)

data class GenerateOrdersRequest(
    val count: Int = 10,
    val customerId: String? = null,
    val itemsPerOrder: Int? = null,
    val triggerSnapshot: Boolean = false,
    val completeOrders: Boolean = true
)

data class GeneratePaymentsRequest(
    val orderIds: List<String>,
    val triggerSnapshot: Boolean = false,
    val triggerDeadLetter: Boolean = false
)

data class GenerateBatchRequest(
    val productCount: Int = 10,
    val orderCount: Int = 50,
    val triggerSnapshots: Boolean = true,
    val triggerDeadLetters: Boolean = true
)
