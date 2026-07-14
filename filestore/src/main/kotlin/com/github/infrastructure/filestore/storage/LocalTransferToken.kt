package com.github.infrastructure.filestore.storage

import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Tiny HMAC-SHA256 token for the local-storage transfer endpoint. Format:
 *
 *     base64url(payload).base64url(hmac-sha256(payload, key))
 *
 * Payload is `bucket|key|contentType|op|exp` where `op` is `PUT` or `GET`.
 * The application server's transfer controller verifies the signature,
 * checks the expiry, and forwards to the local FileStorage.
 */
object LocalTransferToken {
    private const val ALGO = "HmacSHA256"
    private val random = SecureRandom()

    fun issue(
        bucket: String,
        key: String,
        contentType: String,
        expiresIn: Duration,
        write: Boolean,
        signingKey: String,
    ): String {
        val exp = Instant.now().plus(expiresIn).epochSecond
        val nonce = random.nextLong().toString(Character.MAX_RADIX)
        val op = if (write) "PUT" else "GET"
        val payload = "$bucket|$key|$contentType|$op|$exp|$nonce"
        val signature = sign(payload, signingKey)
        return base64Url(payload.toByteArray()) + "." + base64Url(signature)
    }

    fun verify(
        token: String,
        expectedBucket: String,
        expectedKey: String,
        expectedOp: String,
        signingKey: String,
    ): Result {
        val parts = token.split('.', limit = 2)
        if (parts.size != 2) return Result.Invalid("malformed")
        val payloadBytes = runCatching { base64UrlDecode(parts[0]) }.getOrElse { return Result.Invalid("malformed") }
        val signature = runCatching { base64UrlDecode(parts[1]) }.getOrElse { return Result.Invalid("malformed") }
        val payload = String(payloadBytes, Charsets.UTF_8)
        val expected = sign(payload, signingKey)
        if (!constantTimeEquals(signature, expected)) return Result.Invalid("signature mismatch")
        val segments = payload.split('|', limit = 6)
        if (segments.size != 6) return Result.Invalid("payload fields")
        val bucket = segments[0]
        val key = segments[1]
        val contentType = segments[2]
        val op = segments[3]
        val expRaw = segments[4]
        if (bucket != expectedBucket) return Result.Invalid("bucket mismatch")
        if (key != expectedKey) return Result.Invalid("key mismatch")
        if (op != expectedOp) return Result.Invalid("op mismatch")
        val exp = expRaw.toLongOrNull() ?: return Result.Invalid("exp not number")
        if (Instant.ofEpochSecond(exp).isBefore(Instant.now())) return Result.Invalid("expired")
        return Result.Ok(contentType = contentType)
    }

    sealed interface Result {
        data class Ok(val contentType: String) : Result
        data class Invalid(val reason: String) : Result
    }

    private fun sign(payload: String, key: String): ByteArray {
        val mac = Mac.getInstance(ALGO)
        mac.init(SecretKeySpec(key.toByteArray(), ALGO))
        return mac.doFinal(payload.toByteArray())
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].toInt() xor b[i].toInt())
        return result == 0
    }

    private fun base64Url(bytes: ByteArray): String =
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun base64UrlDecode(s: String): ByteArray =
        java.util.Base64.getUrlDecoder().decode(s)
}