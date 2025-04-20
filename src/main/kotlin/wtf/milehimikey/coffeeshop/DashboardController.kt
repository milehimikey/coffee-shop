package wtf.milehimikey.coffeeshop

import org.axonframework.messaging.responsetypes.ResponseTypes
import org.axonframework.queryhandling.QueryGateway
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import wtf.milehimikey.coffeeshop.orders.FindAllOrders
import wtf.milehimikey.coffeeshop.orders.OrderView
import wtf.milehimikey.coffeeshop.payments.FindAllPayments
import wtf.milehimikey.coffeeshop.payments.PaymentView
import wtf.milehimikey.coffeeshop.products.FindAllProducts
import wtf.milehimikey.coffeeshop.products.ProductView

@Controller
class DashboardController(private val queryGateway: QueryGateway) {
    
    @GetMapping("/")
    fun dashboard(model: Model): String {
        // Get all products
        val products = queryGateway.query(
            FindAllProducts(includeInactive = false),
            ResponseTypes.multipleInstancesOf(ProductView::class.java)
        ).join()
        
        // Get all orders
        val orders = queryGateway.query(
            FindAllOrders(),
            ResponseTypes.multipleInstancesOf(OrderView::class.java)
        ).join()
        
        // Get all payments
        val payments = queryGateway.query(
            FindAllPayments(),
            ResponseTypes.multipleInstancesOf(PaymentView::class.java)
        ).join()
        
        model.addAttribute("products", products)
        model.addAttribute("orders", orders)
        model.addAttribute("payments", payments)
        
        return "dashboard"
    }
}
