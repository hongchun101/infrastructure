package com.github.infrastructure.core.web.config

import com.github.infrastructure.core.web.exception.GlobalExceptionHandler
import com.github.infrastructure.core.web.response.RResponseBodyAdvice
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration
class CoreWebAutoConfiguration {
    @Bean
    fun rResponseBodyAdvice(): RResponseBodyAdvice = RResponseBodyAdvice()
    @Bean
    fun globalExceptionHandler(): GlobalExceptionHandler = GlobalExceptionHandler()
}
