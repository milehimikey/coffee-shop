plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "1.9.25"
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

extra["axon.version"] = "4.11.2"

dependencyManagement {
    imports {
        mavenBom("org.axonframework:axon-bom:${property("axon.version")}")
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
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.6")

    // Java Money API (JSR 354)
    implementation("org.javamoney:moneta:1.4.5")
    implementation("org.zalando:jackson-datatype-money:1.3.0")

    // Axon Framework
    implementation("org.axonframework:axon-spring-boot-starter")
    implementation("org.axonframework.extensions.mongo:axon-mongo")
    implementation("org.axonframework.extensions.kotlin:axon-kotlin")
    implementation("org.axonframework:axon-micrometer")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    runtimeOnly("org.postgresql:postgresql")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mongodb")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.axonframework:axon-test")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
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
    annotation("org.axonframework.spring.stereotype.Aggregate")
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
