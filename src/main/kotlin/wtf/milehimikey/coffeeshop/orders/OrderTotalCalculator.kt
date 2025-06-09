package wtf.milehimikey.coffeeshop.orders

import org.javamoney.moneta.Money
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class OrderTotalCalculator {

    fun calculateTotal(items: List<OrderItem>): Money {
        val subTotal = items.fold(
            Money.of(BigDecimal.ZERO, "USD")
        ) { acc, item ->
            acc.add(Money.of(item.price.multiply(BigDecimal(item.quantity)), "USD"))
        }
        return subTotal.add(calculateTax(subTotal))
    }

    private fun calculateTax(total: Money): Money {
        return total.multiply(BigDecimal("0.0825"))
    }
}
