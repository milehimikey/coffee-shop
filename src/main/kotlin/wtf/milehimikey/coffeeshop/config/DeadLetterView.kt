package wtf.milehimikey.coffeeshop.config

import java.time.Instant

/**
 * View class for dead letter entries.
 * Used to represent dead letter entries in the UI and API responses.
 */
data class DeadLetterView(
    val sequenceIdentifier: String,
    val processingGroup: String,
    val causeMessage: String,
    val lastTouched: Instant,
    val diagnostics: Map<String, String>
)
