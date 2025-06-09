package wtf.milehimikey.coffeeshop.config

import org.javamoney.moneta.Money
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@Configuration
class MongoConfiguration {


}

@ReadingConverter
class OrderConverter : Converter<String, Money> {
    override fun convert(source: String): Money {
        return Money.parse(source)
    }
}

@WritingConverter
class OrderTotalCalculator : Converter<Money, String> {
    override fun convert(source: Money): String {
        return source.toString()
    }
}
