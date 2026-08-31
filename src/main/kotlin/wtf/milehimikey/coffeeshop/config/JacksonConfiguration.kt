package wtf.milehimikey.coffeeshop.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.axonframework.conversion.DelegatingGeneralConverter
import org.axonframework.conversion.GeneralConverter
import org.axonframework.conversion.jackson2.Jackson2Converter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.zalando.jackson.datatype.money.MoneyModule

@Configuration
class JacksonConfiguration {

    @Autowired
    fun configureObjectMapper(objectMapper: ObjectMapper) {
        objectMapper.registerModule(MoneyModule())
    }

    /**
     * Axon 5 defaults to a Jackson 3 (`tools.jackson`) ObjectMapper, but the only Money
     * support available - Zalando's [MoneyModule] - is Jackson 2 only. Without this bean,
     * every event carrying a [javax.money.MonetaryAmount] fails to deserialize with
     * "Cannot construct instance of org.javamoney.moneta.Money", silently killing the
     * product and order projections.
     *
     * Delegating to [Jackson2Converter] over the Spring ObjectMapper reuses the mapper that
     * already has [MoneyModule] registered, and matches the converter the command-model
     * tests build their fixtures with.
     *
     * Marked [Primary] because `MessageConverter` and `EventConverter` also implement
     * `Converter`; the Axon Server auto-configuration injects a bare `Converter` by type and
     * would otherwise fail with NoUniqueBeanDefinitionException across the three candidates.
     */
    @Bean
    @Primary
    fun generalConverter(objectMapper: ObjectMapper): GeneralConverter =
        DelegatingGeneralConverter(Jackson2Converter(objectMapper))
}
