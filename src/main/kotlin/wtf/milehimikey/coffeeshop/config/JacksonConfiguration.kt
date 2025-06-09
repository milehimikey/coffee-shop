package wtf.milehimikey.coffeeshop.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.zalando.jackson.datatype.money.MoneyModule

@Configuration
class JacksonConfiguration {

    @Autowired
    fun configureObjectMapper(objectMapper: ObjectMapper) {
        objectMapper.registerModule(MoneyModule())
    }
}
