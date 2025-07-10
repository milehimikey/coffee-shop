package wtf.milehimikey.coffeeshop.config

import org.javamoney.moneta.Money
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import java.math.BigDecimal

@Configuration
class MongoConfiguration {

    @Bean
    fun mongoCustomConversions(): MongoCustomConversions {
        return MongoCustomConversions(
            listOf(
                MoneyWritingConverter(),
                MoneyReadingConverter()
            )
        )
    }
}

@ReadingConverter
class MoneyReadingConverter : Converter<String, Money> {
    override fun convert(source: String): Money {
        return try {
            // First try to parse as Money format (e.g., "USD 3.50")
            Money.parse(source)
        } catch (e: Exception) {
            // If that fails, assume it's a plain number from old BigDecimal storage
            // and convert it to USD Money
            Money.of(BigDecimal(source), "USD")
        }
    }
}

@WritingConverter
class MoneyWritingConverter : Converter<Money, String> {
    override fun convert(source: Money): String {
        // Use the same format as Money.toString() which Money.parse() can handle
        return source.toString()
    }
}
