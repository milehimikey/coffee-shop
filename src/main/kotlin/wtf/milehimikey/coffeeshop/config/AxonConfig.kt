package wtf.milehimikey.coffeeshop.config

import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition
import org.axonframework.eventsourcing.Snapshotter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Axon Framework configuration class.
 * Defines snapshot trigger definitions for different aggregates.
 */
@Configuration
class AxonConfig {

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
}
