package com.github.infrastructure.core.idempotency

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate

/**
 * Redis-backed implementation. Records are stored as JSON strings under
 * the configured key prefix; PENDING and COMPLETE share the slot and are
 * distinguished by the `status` field.
 */
class RedisIdempotencyStore(
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val properties: IdempotencyProperties,
) : IdempotencyStore {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun tryClaim(key: String, payloadHash: String, pendingTtl: Duration): IdempotencyStore.Claim {
        val redisKey = properties.keyPrefix + key
        val pendingJson = objectMapper.writeValueAsString(IdempotencyRecord.Pending(payloadHash))

        val acquired = redis.opsForValue().setIfAbsent(redisKey, pendingJson, pendingTtl)
        if (acquired == true) return IdempotencyStore.Claim.Acquired

        val existing = redis.opsForValue().get(redisKey)
            ?: return IdempotencyStore.Claim.Acquired
        return classify(existing, payloadHash)
    }

    override fun complete(key: String, record: IdempotencyRecord.Complete, completedTtl: Duration) {
        val redisKey = properties.keyPrefix + key
        val json = objectMapper.writeValueAsString(record)
        redis.opsForValue().set(redisKey, json, completedTtl)
    }

    override fun abort(key: String) {
        redis.delete(properties.keyPrefix + key)
    }

    private fun classify(raw: String, payloadHash: String): IdempotencyStore.Claim {
        return try {
            val parsed = objectMapper.readValue(raw, IdempotencyRecord::class.java)
            when (parsed) {
                is IdempotencyRecord.Pending -> {
                    if (parsed.payloadHash == payloadHash) IdempotencyStore.Claim.Conflict.InProgress
                    else IdempotencyStore.Claim.Conflict.PayloadMismatch
                }
                is IdempotencyRecord.Complete -> {
                    if (parsed.payloadHash == payloadHash) IdempotencyStore.Claim.Replay(parsed)
                    else IdempotencyStore.Claim.Conflict.PayloadMismatch
                }
            }
        } catch (e: Exception) {
            log.warn("unparseable idempotency record, treating as conflict: {}", e.message)
            IdempotencyStore.Claim.Conflict.PayloadMismatch
        }
    }
}