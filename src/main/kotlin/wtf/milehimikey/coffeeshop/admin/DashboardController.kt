package wtf.milehimikey.coffeeshop.admin

import org.axonframework.messaging.queryhandling.gateway.QueryGateway
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
        val products = queryGateway.queryMany(FindAllProducts(includeInactive = false), ProductView::class.java).join()
        val orders = queryGateway.queryMany(FindAllOrders(), OrderView::class.java).join()
        val payments = queryGateway.queryMany(FindAllPayments(), PaymentView::class.java).join()

        model.addAttribute("products", products)
        model.addAttribute("orders", orders)
        model.addAttribute("payments", payments)

        return "dashboard"
    }
}
