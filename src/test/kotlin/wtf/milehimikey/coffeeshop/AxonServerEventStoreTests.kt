package wtf.milehimikey.coffeeshop

import io.axoniq.framework.testcontainer.AxonServerContainer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verifies the central claim of the Axon Server migration: events are stored in Axon Server,
 * not in PostgreSQL. Nothing else in the suite asserts this - the other tests would pass
 * identically against the old JPA event store.
 *
 * This is deliberately a second Spring context. `@DirtiesContext(AFTER_CLASS)` disposes the
 * container beans with the context, so this class gets its own Axon Server rather than
 * inheriting events from [CoffeeShopApplicationTests]. That is what keeps the two classes
 * independent; see "Test isolation" in CLAUDE.md before adding a third.
 */
@Import(TestcontainersConfiguration::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
class AxonServerEventStoreTests {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var axonServerContainer: AxonServerContainer

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private val http = RestTemplate()

    @Test
    fun `events are appended to Axon Server and not to PostgreSQL`() {
        val before = axonServerAppendCount()

        val response = restTemplate.postForEntity(
            "/api/products",
            CreateProductRequest(
                name = "Event Store Probe",
                description = "Verifies events land in Axon Server",
                price = BigDecimal("3.10"),
                sku = "PROBE-1"
            ),
            String::class.java
        )
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        // Axon Server must have taken the append
        await().atMost(20, TimeUnit.SECONDS).untilAsserted {
            assertTrue(
                axonServerAppendCount() > before,
                "Axon Server recorded no appended events; is the event store still on JPA?"
            )
        }

        // PostgreSQL must hold no events. The connector ships no TokenStore, so token_entry
        // stays - but any event-store table must be absent or empty.
        val eventRows = jdbcTemplate.queryForObject(
            """
            select coalesce((
                select sum(n_live_tup) from pg_stat_user_tables
                where relname in ('domain_event_entry', 'aggregate_event_entry', 'snapshot_event_entry')
            ), 0)
            """.trimIndent(),
            Long::class.java
        ) ?: 0L
        assertEquals(0L, eventRows, "PostgreSQL event-store tables should hold no rows")

        // The token store, by contrast, must be alive - processors need it
        val tokenRows = jdbcTemplate.queryForObject(
            "select count(*) from token_entry", Long::class.java
        ) ?: 0L
        assertTrue(tokenRows > 0, "expected tracking tokens in PostgreSQL, found none")
    }

    /** Total events appended, read from Axon Server's own actuator metrics. */
    private fun axonServerAppendCount(): Double {
        val url = "http://${axonServerContainer.host}:${axonServerContainer.httpPort}" +
            "/actuator/metrics/axon.events.append.throughput.count"
        return try {
            @Suppress("UNCHECKED_CAST")
            val body = http.getForObject(url, Map::class.java) as Map<String, Any>?
            val measurements = body?.get("measurements") as? List<Map<String, Any>> ?: return 0.0
            (measurements.firstOrNull()?.get("value") as? Number)?.toDouble() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }
}
