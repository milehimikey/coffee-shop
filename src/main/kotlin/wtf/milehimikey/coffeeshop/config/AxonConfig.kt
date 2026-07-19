package wtf.milehimikey.coffeeshop.config

import org.axonframework.extension.spring.config.EventHandlerSelector
import org.axonframework.extension.spring.config.EventProcessorDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AxonConfig(private val deadLetterProcessor: DeadLetterProcessor) {

    @Bean
    fun orderProcessorDefinition(): EventProcessorDefinition =
        EventProcessorDefinition.pooledStreaming("order")
            .assigningHandlers(EventHandlerSelector.matchesNamespaceOnType("wtf.milehimikey.coffeeshop.orders"))
            .customized { config -> config.errorHandler(deadLetterProcessor.errorHandler()) }

    @Bean
    fun paymentProcessorDefinition(): EventProcessorDefinition =
        EventProcessorDefinition.pooledStreaming("payment")
            .assigningHandlers(EventHandlerSelector.matchesNamespaceOnType("wtf.milehimikey.coffeeshop.payments"))
            .customized { config -> config.errorHandler(deadLetterProcessor.errorHandler()) }

    @Bean
    fun productProcessorDefinition(): EventProcessorDefinition =
        EventProcessorDefinition.pooledStreaming("product")
            .assigningHandlers(EventHandlerSelector.matchesNamespaceOnType("wtf.milehimikey.coffeeshop.products"))
            .customized { config -> config.errorHandler(deadLetterProcessor.errorHandler()) }

    /**
     * The JPA/PostgreSQL reporting read model. Separate processor, so it carries its own
     * tracking token and can be replayed without touching the Mongo projections.
     */
    @Bean
    fun reportingProcessorDefinition(): EventProcessorDefinition =
        EventProcessorDefinition.pooledStreaming("reporting")
            .assigningHandlers(EventHandlerSelector.matchesNamespaceOnType("wtf.milehimikey.coffeeshop.reporting"))
            .customized { config -> config.errorHandler(deadLetterProcessor.errorHandler()) }
}
