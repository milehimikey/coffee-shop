package wtf.milehimikey.coffeeshop.config

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pins the thread-naming contract [IdempotencyInterceptor] depends on.
 *
 * The interceptor derives the processing group by parsing the worker thread's name, because
 * Axon exposes no processing group on the ProcessingContext. That makes these formats a real
 * dependency, and they previously drifted silently: the pattern matched only `Processor[...]`,
 * which pooled streaming processors never emit, so every event was filed under the fallback
 * group and the per-group idempotency guarantee quietly did not exist.
 */
class IdempotencyInterceptorTest {

    @Test
    fun `extracts the processing group from a pooled streaming work package thread`() {
        assertEquals(
            "product",
            IdempotencyInterceptor.processingGroupFromThreadName("WorkPackage[product]-0")
        )
    }

    @Test
    fun `extracts the processing group regardless of segment number`() {
        assertEquals(
            "reporting",
            IdempotencyInterceptor.processingGroupFromThreadName("WorkPackage[reporting]-12")
        )
    }

    @Test
    fun `extracts a dotted processor name`() {
        // What the names looked like while the EventProcessorDefinition selectors were inert
        assertEquals(
            "wtf.milehimikey.coffeeshop.products",
            IdempotencyInterceptor.processingGroupFromThreadName(
                "WorkPackage[wtf.milehimikey.coffeeshop.products]-2"
            )
        )
    }

    @Test
    fun `extracts the processing group from a subscribing processor thread`() {
        assertEquals(
            "payment",
            IdempotencyInterceptor.processingGroupFromThreadName("Processor[payment]")
        )
    }

    @Test
    fun `returns null for a thread that is not a processor thread`() {
        assertNull(IdempotencyInterceptor.processingGroupFromThreadName("main"))
        assertNull(IdempotencyInterceptor.processingGroupFromThreadName("Test worker"))
    }
}
