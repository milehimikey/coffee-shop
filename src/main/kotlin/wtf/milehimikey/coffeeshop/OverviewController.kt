package wtf.milehimikey.coffeeshop

import org.axonframework.messaging.responsetypes.ResponseTypes
import org.axonframework.queryhandling.QueryGateway
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import wtf.milehimikey.coffeeshop.orders.FindAllOrders
import wtf.milehimikey.coffeeshop.orders.OrderView
import wtf.milehimikey.coffeeshop.payments.FindAllPayments
import wtf.milehimikey.coffeeshop.payments.PaymentView
import wtf.milehimikey.coffeeshop.products.FindAllProducts
import wtf.milehimikey.coffeeshop.products.ProductView
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/api/overview")
class OverviewController(private val queryGateway: QueryGateway) {
    
    data class SystemOverview(
        val productCount: Int,
        val orderCount: Int,
        val paymentCount: Int,
        val totalSales: String,
        val products: List<ProductView>,
        val orders: List<OrderView>,
        val payments: List<PaymentView>
    )
    
    @GetMapping
    fun getOverview(): CompletableFuture<SystemOverview> {
        val productsQuery = queryGateway.query(
            FindAllProducts(includeInactive = false),
            ResponseTypes.multipleInstancesOf(ProductView::class.java)
        )
        
        val ordersQuery = queryGateway.query(
            FindAllOrders(),
            ResponseTypes.multipleInstancesOf(OrderView::class.java)
        )
        
        val paymentsQuery = queryGateway.query(
            FindAllPayments(),
            ResponseTypes.multipleInstancesOf(PaymentView::class.java)
        )
        
        return CompletableFuture.allOf(productsQuery, ordersQuery, paymentsQuery)
            .thenApply {
                val products = productsQuery.join()
                val orders = ordersQuery.join()
                val payments = paymentsQuery.join()
                
                val totalSales = payments
                    .filter { it.status == "PROCESSED" }
                    .sumOf { it.amount }
                    .toString()
                
                SystemOverview(
                    productCount = products.size,
                    orderCount = orders.size,
                    paymentCount = payments.size,
                    totalSales = "$${totalSales}",
                    products = products,
                    orders = orders,
                    payments = payments
                )
            }
    }
}
