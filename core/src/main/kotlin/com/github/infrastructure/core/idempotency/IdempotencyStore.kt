package com.github.infrastructure.core.idempotency

import java.time.Duration
import java.util.Optional

/**
 * Storage abstraction. The Redis-backed implementation lives in this module;
 * a test fake can be plugged in by replacing the bean.
 */
interface IdempotencyStore {

    /**
     * Attempts to claim a slot for [key] with [payloadHash] for the duration
     * of [pendingTtl]. Returns:
     *
     *  - [Claim.Acquired] if the slot was new (first request); the caller
     *    proceeds to execute the controller and later calls [complete].
     *  - [Claim.Conflict.InProgress] if another in-flight request holds the
     *    slot with the same payload hash.
     *  - [Claim.Conflict.PayloadMismatch] if a different payload previously
     *    used the same key (likely a client bug).
     *  - [Claim.Replay] if the previous response is cached and may be
     *    replayed.
     */
    fun tryClaim(key: String, payloadHash: String, pendingTtl: Duration): Claim

    /**
     * Persists the cached response. TTL is [completedTtl].
     */
    fun complete(key: String, record: IdempotencyRecord.Complete, completedTtl: Duration)

    /**
     * Removes the PENDING marker so the client may retry. Called when the
     * upstream returns a non-cacheable status.
     */
    fun abort(key: String)

    sealed interface Claim {
        data object Acquired : Claim

        sealed interface Conflict : Claim {
            data object InProgress : Conflict
            data object PayloadMismatch : Conflict
        }

        data class Replay(val record: IdempotencyRecord.Complete) : Claim
    }
}