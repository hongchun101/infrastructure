import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    id("com.google.devtools.ksp")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

val jimmerVersion = "0.10.9"

dependencies {
    implementation(project(":core"))
    implementation(project(":security"))
    implementation(project(":observability"))
    implementation(project(":alert"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.babyfish.jimmer:jimmer-spring-boot-starter:$jimmerVersion") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-jdbc")
        exclude(group = "org.springframework.data", module = "spring-data-commons")
    }

    runtimeOnly("org.postgresql:postgresql")

    ksp("org.babyfish.jimmer:jimmer-ksp:$jimmerVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.github.codemonstur:embedded-redis:1.4.3")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.4"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget = JvmTarget.JVM_25
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
