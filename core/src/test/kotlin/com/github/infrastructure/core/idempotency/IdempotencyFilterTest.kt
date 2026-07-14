package com.github.infrastructure.core.idempotency

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.io.ByteArrayInputStream
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class IdempotencyFilterTest {

    private val objectMapper = ObjectMapper()
    private val properties = IdempotencyProperties().apply {
        pathPrefixes = listOf("/api/")
        maxKeyLength = 64
        cacheableStatusRange = 200..299
        pendingTtlSeconds = 60
        completedTtlSeconds = 3600
    }

    private class FakeStore : IdempotencyStore {
        val claimed = mutableMapOf<String, IdempotencyRecord>()
        var completeCalls = 0
        var abortCalls = 0

        override fun tryClaim(key: String, payloadHash: String, pendingTtl: Duration): IdempotencyStore.Claim {
            val existing = claimed[key]
            return when (existing) {
                null -> {
                    claimed[key] = IdempotencyRecord.Pending(payloadHash)
                    IdempotencyStore.Claim.Acquired
                }
                is IdempotencyRecord.Pending -> if (existing.payloadHash == payloadHash) {
                    IdempotencyStore.Claim.Conflict.InProgress
                } else {
                    IdempotencyStore.Claim.Conflict.PayloadMismatch
                }
                is IdempotencyRecord.Complete -> if (existing.payloadHash == payloadHash) {
                    IdempotencyStore.Claim.Replay(existing)
                } else {
                    IdempotencyStore.Claim.Conflict.PayloadMismatch
                }
            }
        }

        override fun complete(key: String, record: IdempotencyRecord.Complete, completedTtl: Duration) {
            claimed[key] = record
            completeCalls++
        }

        override fun abort(key: String) {
            claimed.remove(key)
            abortCalls++
        }
    }

    private fun chainWithJsonEcho(): Pair<FilterChain, AtomicInteger> {
        val calls = AtomicInteger(0)
        val chain = FilterChain { _: ServletRequest, response: ServletResponse ->
            calls.incrementAndGet()
            val resp = response as HttpServletResponse
            resp.status = 201
            resp.contentType = "application/json"
            resp.writer.write("""{"echo":"ok"}""")
        }
        return chain to calls
    }

    private fun chainThat(status: Int, body: String): Pair<FilterChain, AtomicInteger> {
        val calls = AtomicInteger(0)
        val chain = FilterChain { _, response ->
            calls.incrementAndGet()
            val resp = response as HttpServletResponse
            resp.status = status
            resp.contentType = "application/json"
            resp.writer.write(body)
        }
        return chain to calls
    }

    private fun postWithBody(path: String, key: String?, body: String): MockHttpServletRequest {
        val req = MockHttpServletRequest("POST", path)
        if (key != null) req.addHeader("Idempotency-Key", key)
        req.setContent(body.toByteArray())
        return req
    }

    @Test
    fun `no header means filter is transparent`() {
        val store = FakeStore()
        val filter = IdempotencyFilter(properties, store, objectMapper)
        val req = postWithBody("/api/payments", null, """{"amount":100}""")
        val resp = MockHttpServletResponse()
        val (chain, calls) = chainWithJsonEcho()

        filter.doFilter(req, resp, chain)

        assertThat(calls.get()).isEqualTo(1)
        assertThat(resp.status).isEqualTo(201)
        assertThat(store.claimed).isEmpty()
    }

    @Test
    fun `first call acquires the slot and caches the response`() {
        val store = FakeStore()
        val filter = IdempotencyFilter(properties, store, objectMapper)
        val req = postWithBody("/api/payments", "key-1", """{"amount":100}""")
        val resp = MockHttpServletResponse()
        val (chain, _) = chainWithJsonEcho()

        filter.doFilter(req, resp, chain)

        assertThat(store.completeCalls).isEqualTo(1)
        val record = store.claimed["key-1"]
        assertThat(record).isInstanceOf(IdempotencyRecord.Complete::class.java)
        val complete = record as IdempotencyRecord.Complete
        assertThat(complete.status).isEqualTo(201)
        assertThat(complete.body).contains("ok")
        assertThat(resp.getHeader("Idempotent-Replayed")).isNull()
    }

    @Test
    fun `replay returns cached body and sets replayed header`() {
        val store = FakeStore()
        val filter = IdempotencyFilter(properties, store, objectMapper)

        // First call: cache
        val req1 = postWithBody("/api/payments", "key-1", """{"amount":100}""")
        val resp1 = MockHttpServletResponse()
        val (chain1, _) = chainWithJsonEcho()
        filter.doFilter(req1, resp1, chain1)

        // Second call: replay
        val req2 = postWithBody("/api/payments", "key-1", """{"amount":100}""")
        val resp2 = MockHttpServletResponse()
        val (chain2, calls2) = chainWithJsonEcho()

        filter.doFilter(req2, resp2, chain2)

        assertThat(calls2.get()).isEqualTo(0)
        assertThat(resp2.status).isEqualTo(201)
        assertThat(resp2.getHeader("Idempotent-Replayed")).isEqualTo("true")
        assertThat(resp2.contentAsString).contains("ok")
    }

    @Test
    fun `same key with different payload returns 422`() {
        val store = FakeStore()
        val filter = IdempotencyFilter(properties, store, objectMapper)

        val req1 = postWithBody("/api/payments", "key-1", """{"amount":100}""")
        val resp1 = MockHttpServletResponse()
        val (chain1, _) = chainWithJsonEcho()
        filter.doFilter(req1, resp1, chain1)

        val req2 = postWithBody("/api/payments", "key-1", """{"amount":999}""")
        val resp2 = MockHttpServletResponse()
        val (chain2, calls2) = chainWithJsonEcho()
        filter.doFilter(req2, resp2, chain2)

        assertThat(calls2.get()).isEqualTo(0)
        assertThat(resp2.status).isEqualTo(422)
        assertThat(resp2.contentAsString).contains("different payload")
    }

    @Test
    fun `concurrent in-progress request returns 409`() {
        val store = FakeStore()
        val filter = IdempotencyFilter(properties, store, objectMapper)

        val payload = """{"amount":100}"""
        val expectedHash = sha256Hex(payload.toByteArray())
        store.claimed["key-1"] = IdempotencyRecord.Pending(expectedHash)

        val req = postWithBody("/api/payments", "key-1", payload)
        val resp = MockHttpServletResponse()
        val (chain, calls) = chainWithJsonEcho()
        filter.doFilter(req, resp, chain)

        assertThat(calls.get()).isEqualTo(0)
        assertThat(resp.status).isEqualTo(409)
        assertThat(resp.contentAsString).contains("in progress")
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `4xx response clears the pending slot`() {
        val store = FakeStore()
        val filter = IdempotencyFilter(properties, store, objectMapper)

        val req = postWithBody("/api/payments", "key-1", """{"amount":100}""")
        val resp = MockHttpServletResponse()
        val (chain, _) = chainThat(422, """{"error":"bad"}""")
        filter.doFilter(req, resp, chain)

        assertThat(store.abortCalls).isEqualTo(1)
        assertThat(store.claimed).isEmpty()
    }

    @Test
    fun `get request bypasses filter`() {
        val store = FakeStore()
        val filter = IdempotencyFilter(properties, store, objectMapper)

        val req = MockHttpServletRequest("GET", "/api/payments/42")
        req.addHeader("Idempotency-Key", "key-1")
        val resp = MockHttpServletResponse()
        val (chain, calls) = chainWithJsonEcho()
        filter.doFilter(req, resp, chain)

        assertThat(calls.get()).isEqualTo(1)
        assertThat(store.claimed).isEmpty()
    }

    @Test
    fun `over-long key returns 400`() {
        val store = FakeStore()
        val properties = IdempotencyProperties().apply { maxKeyLength = 8 }
        val filter = IdempotencyFilter(properties, store, objectMapper)

        val req = postWithBody("/api/payments", "this-key-is-way-too-long", "{}")
        val resp = MockHttpServletResponse()
        val (chain, calls) = chainWithJsonEcho()
        filter.doFilter(req, resp, chain)

        assertThat(calls.get()).isEqualTo(0)
        assertThat(resp.status).isEqualTo(400)
        assertThat(store.claimed).isEmpty()
    }

    @Test
    fun `controller can still read the request body`() {
        val store = FakeStore()
        val filter = IdempotencyFilter(properties, store, objectMapper)

        val req = postWithBody("/api/payments", "key-1", """{"amount":42}""")
        val resp = MockHttpServletResponse()
        val seenBody = StringBuilder()
        val chain = FilterChain { request, response ->
            val body = request.inputStream.readBytes().toString(Charsets.UTF_8)
            seenBody.append(body)
            val r = response as HttpServletResponse
            r.status = 201
            r.writer.write("""{"ok":true}""")
        }
        filter.doFilter(req, resp, chain)

        assertThat(seenBody.toString()).isEqualTo("""{"amount":42}""")
        assertThat(store.completeCalls).isEqualTo(1)
    }
}

private class StubInputStream(private val bytes: ByteArray) : ServletInputStream() {
    private val delegate = ByteArrayInputStream(bytes)
    override fun read(): Int = delegate.read()
    override fun isFinished(): Boolean = delegate.available() == 0
    override fun isReady(): Boolean = true
    override fun setReadListener(readListener: ReadListener?) {
        throw UnsupportedOperationException()
    }
}