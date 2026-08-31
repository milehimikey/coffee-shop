package wtf.milehimikey.coffeeshop.config

import org.axonframework.extension.spring.config.EventHandlerSelector
import org.axonframework.extension.spring.config.EventProcessorDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AxonConfig(private val deadLetterProcessor: DeadLetterProcessor) {

    /**
     * Selects event handlers by the Java package their bean type lives in.
     *
     * These definitions previously used [EventHandlerSelector.matchesNamespaceOnType], which
     * despite the package-shaped argument does not match Java packages at all: it looks for a
     * `namespace` *attribute* on an annotation on the handler type. Nothing here declares one,
     * so no selector ever matched, every definition below was silently inert, and handlers fell
     * back to per-package default processors carrying the default propagating error handler.
     *
     * Two things were broken by that: the custom [DeadLetterProcessor] error handler was never
     * installed, so failing events aborted the work package instead of being recorded; and the
     * `axon.eventhandling.processors.*` settings key on processor name, so they applied to
     * processors that did not exist.
     */
    private fun handlersInPackage(packageName: String) =
        EventHandlerSelector { descriptor -> descriptor.beanType()?.packageName == packageName }

    @Bean
    fun orderProcessorDefinition(): EventProcessorDefinition =
        EventProcessorDefinition.pooledStreaming("order")
            .assigningHandlers(handlersInPackage("wtf.milehimikey.coffeeshop.orders"))
            .customized { config -> config.errorHandler(deadLetterProcessor.errorHandler()) }

    @Bean
    fun paymentProcessorDefinition(): EventProcessorDefinition =
        EventProcessorDefinition.pooledStreaming("payment")
            .assigningHandlers(handlersInPackage("wtf.milehimikey.coffeeshop.payments"))
            .customized { config -> config.errorHandler(deadLetterProcessor.errorHandler()) }

    @Bean
    fun productProcessorDefinition(): EventProcessorDefinition =
        EventProcessorDefinition.pooledStreaming("product")
            .assigningHandlers(handlersInPackage("wtf.milehimikey.coffeeshop.products"))
            .customized { config -> config.errorHandler(deadLetterProcessor.errorHandler()) }

    /**
     * The JPA/PostgreSQL reporting read model. Separate processor, so it carries its own
     * tracking token and can be replayed without touching the Mongo projections.
     */
    @Bean
    fun reportingProcessorDefinition(): EventProcessorDefinition =
        EventProcessorDefinition.pooledStreaming("reporting")
            .assigningHandlers(handlersInPackage("wtf.milehimikey.coffeeshop.reporting"))
            .customized { config -> config.errorHandler(deadLetterProcessor.errorHandler()) }
}
