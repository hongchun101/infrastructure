package com.github.infrastructure.core.idempotency

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "infrastructure.idempotency")
class IdempotencyProperties {

    /**
     * Master switch. When false the filter is not registered and the feature
     * is effectively disabled.
     */
    var enabled: Boolean = true

    /**
     * HTTP header that carries the idempotency key.
     */
    var headerName: String = "Idempotency-Key"

    /**
     * Path prefixes this filter applies to. Anything else is passed through.
     */
    var pathPrefixes: List<String> = listOf("/api/")

    /**
     * TTL for the PENDING marker. Should be longer than the slowest expected
     * request so that "in progress" detection does not race ahead of the
     * actual completion.
     */
    var pendingTtlSeconds: Long = 60

    /**
     * TTL for a completed (cached response) record. Replay works within this
     * window. Reasonable default: an hour.
     */
    var completedTtlSeconds: Long = 3600

    /**
     * Hard cap on key length to avoid unbounded memory use in Redis.
     */
    var maxKeyLength: Int = 255

    /**
     * Reject caching of responses larger than this many bytes; instead the
     * PENDING marker expires naturally and the next replay retries the work.
     */
    var maxCachedBodyBytes: Int = 1 * 1024 * 1024

    /**
     * Status codes that are eligible for caching as a successful replay.
     * Defaults to all 2xx.
     */
    var cacheableStatusRange: IntRange = 200..299

    /**
     * HTTP methods this filter engages on. Defaults to POST only.
     */
    var methods: Set<String> = setOf("POST")

    /**
     * Redis key prefix.
     */
    var keyPrefix: String = "idem:"

    /**
     * Whether to include the response body in the cached record. When false
     * only the status code is replayed with an empty body; useful for
     * expensive payloads where the client can refetch.
     */
    var cacheResponseBody: Boolean = true
}