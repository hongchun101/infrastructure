plugins {
    id("org.springframework.boot") version "3.5.14" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("jvm") version "2.3.0" apply false
    kotlin("plugin.spring") version "2.3.0" apply false
    id("com.google.devtools.ksp") version "2.3.9" apply false
}

group = "com.github"
version = "0.0.1-SNAPSHOT"
extra["kotlin.version"] = "2.3.0"

allprojects {
    group = rootProject.group
    version = rootProject.version
}

subprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion("2.3.0")
            }
        }
    }
}
