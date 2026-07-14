package com.github.infrastructure.app.announcement.event

import java.util.UUID

data class AnnouncementPublishedEvent(
    val announcementId: UUID,
    val title: String,
    val summary: String?,
    val content: String,
    val priority: Int,
    val publishedByName: String?,
)