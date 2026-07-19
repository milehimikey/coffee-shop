plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "2.1.0"
    id("com.google.cloud.tools.jib") version "3.4.5"
}

group = "wtf.milehimikey"
version = "0.0.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["axon.version"] = "5.1.0"
extra["testcontainers.version"] = "1.21.4"
dependencyManagement {
    imports {
        mavenBom("org.axonframework:axon-framework-bom:${property("axon.version")}")
    }
    dependencies {
        // Axon 5's Jackson 3 converter requires com.fasterxml.jackson.annotation.JsonSerializeAs,
        // which only exists from jackson-annotations 2.21. Spring Boot 3.5.0 otherwise pins the
        // Jackson 2 stack to 2.19.0 and startup fails with NoClassDefFoundError.
        dependency("com.fasterxml.jackson.core:jackson-annotations:2.21")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Micrometer Prometheus registry
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Java Money API (JSR 354)
    implementation("org.javamoney:moneta:1.4.5")
    implementation("org.zalando:jackson-datatype-money:1.3.0")

    // Axon Framework
    implementation("org.axonframework.extensions.spring:axon-spring-boot-starter")
    implementation("org.axonframework.extensions.metrics:axon-metrics-micrometer")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.testcontainers:junit-jupiter:${property("testcontainers.version")}")
    testImplementation("org.testcontainers:mongodb:${property("testcontainers.version")}")
    testImplementation("org.testcontainers:postgresql:${property("testcontainers.version")}")
    testImplementation("org.axonframework:axon-test")
    testImplementation("org.awaitility:awaitility:4.2.2")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
    annotation("org.axonframework.extension.spring.stereotype.EventSourced")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jib {
    from {
        image = "bitnami/java:21"
    }
    to {
        image = "milehimikey/${rootProject.name}"
        tags = setOf("${project.version}", "latest")
    }
    container {
        ports = listOf("9090")
        jvmFlags = listOf("-Xms512m", "-Xmx512m")
        mainClass = "wtf.milehimikey.coffeeshop.CoffeeShopApplicationKt"
        environment = mapOf(
            "JAVA_TOOL_OPTIONS" to "-XX:+UseContainerSupport"
        )
        labels.set(mapOf(
            "maintainer" to "MiKey <milehimikey@gmail.com>",
            "org.opencontainers.image.title" to rootProject.name,
            "org.opencontainers.image.version" to project.version.toString(),
            "org.opencontainers.image.description" to "Coffee Shop Application"
        ))
    }
}
