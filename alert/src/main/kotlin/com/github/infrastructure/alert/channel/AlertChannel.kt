package com.github.infrastructure.alert.channel

import com.fasterxml.jackson.databind.JsonNode
import com.github.infrastructure.alert.entity.AlertChannelType

/**
 * Result of a single send attempt.
 */
data class ChannelSendOutcome(
    val httpStatus: Int?,
    val errorMessage: String?,
)

interface AlertChannel {
    val type: AlertChannelType

    /**
     * Send the payload to the given target. Implementations must never throw
     * — failures are reported via [ChannelSendOutcome.errorMessage].
     */
    fun send(target: String, payload: JsonNode): ChannelSendOutcome
}
