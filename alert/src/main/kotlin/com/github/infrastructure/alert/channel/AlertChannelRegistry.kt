package com.github.infrastructure.alert.channel

import com.github.infrastructure.alert.entity.AlertChannelType
import org.springframework.stereotype.Component

@Component
class AlertChannelRegistry(
    channels: List<AlertChannel>,
) {
    private val byType: Map<AlertChannelType, AlertChannel> =
        channels.associateBy { it.type }

    fun get(type: AlertChannelType): AlertChannel? = byType[type]

    fun types(): Set<AlertChannelType> = byType.keys
}
