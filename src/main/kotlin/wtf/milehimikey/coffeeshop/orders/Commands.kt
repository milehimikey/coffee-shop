package wtf.milehimikey.coffeeshop.orders

import org.axonframework.modelling.annotation.TargetEntityId
import org.javamoney.moneta.Money
import java.util.*

data class CreateOrder(
    @TargetEntityId val id: String = UUID.randomUUID().toString(),
    val customerId: String
)

data class AddItemToOrder(
    @TargetEntityId val orderId: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val price: Money
)

data class SubmitOrder(
    @TargetEntityId val orderId: String
)

data class DeliverOrder(
    @TargetEntityId val orderId: String
)

data class CompleteOrder(
    @TargetEntityId val orderId: String
)

data class CorrectOrderItemProductName(
    @TargetEntityId val orderId: String,
    val productId: String,
    val correctedProductName: String
)
