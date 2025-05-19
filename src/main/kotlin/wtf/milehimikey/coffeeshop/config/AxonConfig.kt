package wtf.milehimikey.coffeeshop.config

import org.axonframework.config.Configurer
import org.axonframework.config.ConfigurerModule
import org.axonframework.config.EventProcessingConfigurer
import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition
import org.axonframework.eventsourcing.Snapshotter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Axon Framework configuration class.
 * Defines snapshot trigger definitions for different aggregates and dead letter queue configuration.
 */
@Configuration
class AxonConfig(private val idempotencyInterceptor: IdempotencyInterceptor) {

    /**
     * Defines a snapshot trigger for the Order aggregate.
     * This will create a snapshot after 50 events have been processed.
     * Orders typically have many events, so a moderate threshold is appropriate.
     */
    @Bean
    fun orderSnapshotTriggerDefinition(snapshotter: Snapshotter) =
        EventCountSnapshotTriggerDefinition(snapshotter, 50)

    /**
     * Defines a snapshot trigger for the Product aggregate.
     * This will create a snapshot after 200 events have been processed.
     * Products change less frequently, so a higher threshold is appropriate.
     */
    @Bean
    fun productSnapshotTriggerDefinition(snapshotter: Snapshotter) =
        EventCountSnapshotTriggerDefinition(snapshotter, 200)

    /**
     * Defines a snapshot trigger for the Payment aggregate.
     * This will create a snapshot after 25 events have been processed.
     * Payments are critical financial records, so a lower threshold is appropriate.
     */
    @Bean
    fun paymentSnapshotTriggerDefinition(snapshotter: Snapshotter) =
        EventCountSnapshotTriggerDefinition(snapshotter, 25)

    // Dead letter queue configuration is now done in application.yml
    // with the property: axon.eventhandling.processors.payment.dlq.enabled=true

    /**
     * Configures the IdempotencyInterceptor for all pooled event processors.
     * This interceptor ensures that events are processed exactly once by tracking
     * processed events using their entity ID and event ID.
     */
    @Bean
    fun idempotencyInterceptorConfigurer(): ConfigurerModule {
        return ConfigurerModule { configurer: Configurer ->
            configurer.eventProcessing { processingConfigurer: EventProcessingConfigurer ->
                // Register the idempotency interceptor for all pooled processors
                // The list of processor names should match those defined in application.yml
                val processorNames = listOf("order", "payment", "product")

                processorNames.forEach { processorName ->
                    processingConfigurer.registerHandlerInterceptor(
                        processorName
                    ) { _ -> idempotencyInterceptor }
                }
            }
        }
    }
}
