package com.github.infrastructure.alert.channel

import org.slf4j.LoggerFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory

internal object WebhookHttpClientFactory {
    private val log = LoggerFactory.getLogger(javaClass)
    fun newRequestFactory(): SimpleClientHttpRequestFactory = try {
        val factory = SimpleClientHttpRequestFactory()
        factory.setConnectTimeout(5000)
        factory.setReadTimeout(5000)
        factory
    } catch (e: Exception) {
        log.warn("failed to build request factory, falling back to default", e)
        SimpleClientHttpRequestFactory()
    }
}
