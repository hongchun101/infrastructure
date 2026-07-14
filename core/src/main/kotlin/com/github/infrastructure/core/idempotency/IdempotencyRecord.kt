package com.github.infrastructure.core.idempotency

/**
 * Internal storage record. The Redis value is the JSON form of this class.
 *
 * Lifecycle:
 *
 *   PENDING(payloadHash)             -- written on first arrival
 *   COMPLETE(payloadHash, status, body, replayedAt)
 *                                     -- written after the controller returns a 2xx
 *   (deleted)                        -- when the controller returns 4xx/5xx; client may retry
 */
sealed interface IdempotencyRecord {

    val payloadHash: String

    data class Pending(
        override val payloadHash: String,
    ) : IdempotencyRecord

    data class Complete(
        override val payloadHash: String,
        val status: Int,
        val contentType: String?,
        val body: String?,
    ) : IdempotencyRecord
}