package wtf.milehimikey.coffeeshop.products

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Document(collection = "products")
data class ProductDocument(
    @Id val id: String,
    val name: String,
    val description: String,
    val price: BigDecimal,
    val active: Boolean = true
)

@Repository
interface ProductRepository : MongoRepository<ProductDocument, String> {
    fun findByActiveTrue(): List<ProductDocument>
}
