package com.github.infrastructure.core.web

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration
class CoreWebAutoConfiguration {
    @Bean
    fun rResponseBodyAdvice(): RResponseBodyAdvice = RResponseBodyAdvice()

    @Bean
    fun globalExceptionHandler(): GlobalExceptionHandler = GlobalExceptionHandler()
}
