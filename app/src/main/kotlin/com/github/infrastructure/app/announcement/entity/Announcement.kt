package com.github.infrastructure.app.announcement.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.LogicalDeleted
import org.babyfish.jimmer.sql.Table
import java.time.LocalDateTime
import java.util.UUID

enum class AnnouncementStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED,
}

@Entity
@Table(name = "announcements")
interface Announcement {
    @Id
    val id: UUID
    val title: String
    val summary: String?
    val content: String
    val status: String
    val priority: Int
    val publishedAt: LocalDateTime?
    val publishAt: LocalDateTime?
    val createdBy: UUID
    val updatedBy: UUID
    val createdTime: LocalDateTime
    val updatedTime: LocalDateTime

    @LogicalDeleted("now")
    val deletedAt: LocalDateTime?
}
