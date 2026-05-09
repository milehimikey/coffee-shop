package wtf.milehimikey.coffeeshop.config

import org.axonframework.extension.spring.config.EventHandlerSelector
import org.axonframework.extension.spring.config.EventProcessorDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AxonConfig {

    @Bean
    fun orderProcessorDefinition(): EventProcessorDefinition =
        EventProcessorDefinition.pooledStreaming("order")
            .assigningHandlers(EventHandlerSelector.matchesNamespaceOnType("wtf.milehimikey.coffeeshop.orders"))
            .notCustomized()

    @Bean
    fun paymentProcessorDefinition(): EventProcessorDefinition =
        EventProcessorDefinition.pooledStreaming("payment")
            .assigningHandlers(EventHandlerSelector.matchesNamespaceOnType("wtf.milehimikey.coffeeshop.payments"))
            .notCustomized()

    @Bean
    fun productProcessorDefinition(): EventProcessorDefinition =
        EventProcessorDefinition.pooledStreaming("product")
            .assigningHandlers(EventHandlerSelector.matchesNamespaceOnType("wtf.milehimikey.coffeeshop.products"))
            .notCustomized()
}
