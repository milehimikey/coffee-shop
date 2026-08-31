package wtf.milehimikey.coffeeshop

import io.axoniq.framework.testcontainer.AxonServerContainer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    fun mongoDbContainer(): MongoDBContainer {
        return MongoDBContainer(DockerImageName.parse("mongo:latest"))
    }

    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> {
        // Pinned for the same reason as compose.yaml: 'latest' plus a data directory breaks
        // across major versions.
        return PostgreSQLContainer(DockerImageName.parse("postgres:17"))
    }

    /**
     * Axon Server for integration tests. `withDcbContext(true)` is required - Axon Framework 5
     * needs a DCB-enabled context, and a default-initialised node does not have one.
     *
     * Wired by `AxonServerTestContainerConnectionDetailsFactory`, which is typed against this
     * container class, so `@ServiceConnection` supplies the address without a
     * `@DynamicPropertySource`.
     */
    @Bean
    @ServiceConnection
    fun axonServerContainer(): AxonServerContainer {
        return AxonServerContainer(DockerImageName.parse("axoniq/axonserver:2026.0.4"))
            .withDevMode(true)
            .withDcbContext(true)
    }
}
