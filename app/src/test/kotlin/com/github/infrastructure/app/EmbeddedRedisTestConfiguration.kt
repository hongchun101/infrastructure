package com.github.infrastructure.app

import jakarta.annotation.PreDestroy
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import redis.embedded.RedisServer

@TestConfiguration(proxyBeanMethods = false)
class EmbeddedRedisTestConfiguration {
    private val redisServer: RedisServer = RedisServer.newRedisServer()
        .port(16379)
        .bind("127.0.0.1")
        .setting("maxmemory 128M")
        .build()
        .also { it.start() }

    @Bean
    fun embeddedRedisServer(): RedisServer = redisServer

    @PreDestroy
    fun stopRedis() {
        redisServer.stop()
    }
}
