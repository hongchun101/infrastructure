package com.github.infrastructure.core.idempotency

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServletResponseWrapper
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingResponseWrapper

/**
 * Idempotency-Key aware servlet filter. See [IdempotencyProperties] for
 * configuration knobs.
 *
 * Flow:
 *
 *   1. Header present, method/path allowed -> hash request body and call
 *      store.tryClaim().
 *   2. Acquired: wrap the response, run the chain, persist the result if
 *      status is cacheable.
 *   3. Conflict.InProgress: 409.
 *   4. Conflict.PayloadMismatch: 409 with explanatory body.
 *   5. Replay: write the cached status + body back to the response with
 *      `Idempotent-Replayed: true`.
 *
 * Bodies larger than [IdempotencyProperties.maxCachedBodyBytes] are not
 * cached; only the status is replayed. Callers that need the full body
 * should keep payloads small or refetch via a follow-up GET.
 */
class IdempotencyFilter(
    private val properties: IdempotencyProperties,
    private val store: IdempotencyStore,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        if (!properties.enabled) return true
        if (request.method !in properties.methods) return true
        val path = request.requestURI
        return properties.pathPrefixes.none { path.startsWith(it) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val key = request.getHeader(properties.headerName)?.trim().orEmpty()
        if (key.isEmpty()) {
            chain.doFilter(request, response)
            return
        }
        if (key.length > properties.maxKeyLength) {
            writeError(response, HttpStatus.BAD_REQUEST, "idempotency key too long (max ${properties.maxKeyLength})")
            return
        }

        val bodyBytes = readBody(request)
        val wrappedRequest = CachedBodyRequestWrapper(request, bodyBytes)
        val payloadHash = sha256Hex(bodyBytes)

        when (val claim = store.tryClaim(key, payloadHash, Duration.ofSeconds(properties.pendingTtlSeconds))) {
            is IdempotencyStore.Claim.Acquired -> executeAndStore(wrappedRequest, response, chain, key, bodyBytes)
            is IdempotencyStore.Claim.Conflict -> {
                val message = when (claim) {
                    IdempotencyStore.Claim.Conflict.InProgress -> "request with idempotency key '$key' is in progress"
                    IdempotencyStore.Claim.Conflict.PayloadMismatch -> "idempotency key '$key' was previously used with a different payload"
                }
                val status = when (claim) {
                    IdempotencyStore.Claim.Conflict.InProgress -> HttpStatus.CONFLICT
                    IdempotencyStore.Claim.Conflict.PayloadMismatch -> HttpStatus.UNPROCESSABLE_ENTITY
                }
                writeError(response, status, message)
            }
            is IdempotencyStore.Claim.Replay -> replay(response, claim.record)
        }
    }

    private fun readBody(request: HttpServletRequest): ByteArray {
        // Cache upstream of the controller binding. The input stream is
        // single-pass; the controller will see the same bytes via the
        // CachedBodyRequestWrapper we hand it below.
        return request.inputStream.readBytes()
    }

    private fun executeAndStore(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
        key: String,
        bodyBytes: ByteArray,
    ) {
        val wrapped = ContentCachingResponseWrapper(response)
        try {
            chain.doFilter(request, wrapped)
        } catch (e: Exception) {
            // Upstream blew up: clear the marker so the client can retry
            store.abort(key)
            throw e
        }

        val status = wrapped.status
        val cachedBody: ByteArray? = wrapped.contentAsByteArray.takeIf {
            it.isNotEmpty() && properties.cacheResponseBody && it.size <= properties.maxCachedBodyBytes
        }

        when {
            status in properties.cacheableStatusRange && cachedBody != null -> {
                val payloadHash = sha256Hex(bodyBytes)
                store.complete(
                    key,
                    IdempotencyRecord.Complete(
                        payloadHash = payloadHash,
                        status = status,
                        contentType = wrapped.contentType ?: MediaType.APPLICATION_JSON_VALUE,
                        body = String(cachedBody, StandardCharsets.UTF_8),
                    ),
                    Duration.ofSeconds(properties.completedTtlSeconds),
                )
            }
            status in properties.cacheableStatusRange && cachedBody == null -> {
                // Body too large: cache status only with a stub body marker.
                val payloadHash = sha256Hex(bodyBytes)
                store.complete(
                    key,
                    IdempotencyRecord.Complete(
                        payloadHash = payloadHash,
                        status = status,
                        contentType = wrapped.contentType ?: MediaType.APPLICATION_JSON_VALUE,
                        body = null,
                    ),
                    Duration.ofSeconds(properties.completedTtlSeconds),
                )
            }
            status >= 400 -> {
                store.abort(key)
            }
            else -> {
                // 1xx/3xx etc: leave the PENDING to expire naturally.
            }
        }
        wrapped.copyBodyToResponse()
    }

    private fun replay(response: HttpServletResponse, record: IdempotencyRecord.Complete) {
        response.status = record.status
        response.setHeader(HttpHeaders.CONTENT_TYPE, record.contentType ?: MediaType.APPLICATION_JSON_VALUE)
        response.setHeader("Idempotent-Replayed", "true")
        val body = record.body
        if (!body.isNullOrEmpty()) {
            response.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
        }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        if (bytes.isEmpty()) return EMPTY_HASH
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    private fun writeError(response: HttpServletResponse, status: HttpStatus, message: String) {
        response.status = status.value()
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        val payload = objectMapper.writeValueAsBytes(
            mapOf(
                "code" to status.value(),
                "message" to message,
            ),
        )
        response.outputStream.use { it.write(payload) }
    }

    /**
     * Replayable request wrapper: the body has already been consumed once by
     * this filter for hashing, but the controller still needs to deserialize
     * it. We hand it back the same bytes via a fresh ByteArrayInputStream.
     */
    private class CachedBodyRequestWrapper(
        request: HttpServletRequest,
        private val body: ByteArray,
    ) : HttpServletRequestWrapper(request) {

        override fun getInputStream(): ServletInputStream = CachedServletInputStream(body)

        @Throws(IOException::class)
        override fun getReader(): BufferedReader =
            BufferedReader(InputStreamReader(ByteArrayInputStream(body), StandardCharsets.UTF_8))
    }

    private class CachedServletInputStream(
        private val body: ByteArray,
    ) : ServletInputStream() {
        private val delegate = ByteArrayInputStream(body)
        override fun read(): Int = delegate.read()
        override fun isFinished(): Boolean = delegate.available() == 0
        override fun isReady(): Boolean = true
        override fun setReadListener(readListener: ReadListener?) {
            throw UnsupportedOperationException("async reads are not supported")
        }
    }

    companion object {
        private val EMPTY_HASH = MessageDigest.getInstance("SHA-256").digest().joinToString("") { "%02x".format(it) }
    }
}