package wtf.milehimikey.coffeeshop.config

import org.javamoney.moneta.Money
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions

@Configuration
class MongoConfiguration {

    @Bean
    fun mongoCustomConversions(): MongoCustomConversions {
        return MongoCustomConversions(
            listOf(
                MoneyReadingConverter(),
                MoneyWritingConverter()
            )
        )
    }
}

@ReadingConverter
class MoneyReadingConverter : Converter<String, Money> {
    override fun convert(source: String): Money {
        return Money.parse(source)
    }
}

@WritingConverter
class MoneyWritingConverter : Converter<Money, String> {
    override fun convert(source: Money): String {
        return source.toString()
    }
}
