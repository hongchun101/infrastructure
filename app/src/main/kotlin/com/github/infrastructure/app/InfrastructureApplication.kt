package com.github.infrastructure.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.github.infrastructure"])
class InfrastructureApplication

fun main(args: Array<String>) {
    runApplication<InfrastructureApplication>(*args)
}
