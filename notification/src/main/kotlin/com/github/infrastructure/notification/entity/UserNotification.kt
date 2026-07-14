package com.github.infrastructure.notification.entity

import java.time.LocalDateTime
import java.util.UUID
import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "user_notifications")
interface UserNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Column(name = "recipient_user_id")
    val recipientUserId: UUID

    val category: String

    val title: String

    val content: String

    @Column(name = "link_url")
    val linkUrl: String?

    val payload: String?

    val priority: Int

    @Column(name = "read_at")
    val readAt: LocalDateTime?

    @Column(name = "archived_at")
    val archivedAt: LocalDateTime?

    @Column(name = "created_at")
    val createdAt: LocalDateTime

    @Column(name = "expires_at")
    val expiresAt: LocalDateTime?
}