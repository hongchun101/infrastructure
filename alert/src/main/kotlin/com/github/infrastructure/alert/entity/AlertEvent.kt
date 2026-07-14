package com.github.infrastructure.alert.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "alert_events")
interface AlertEvent {
    @Id
    val id: UUID

    @Key
    val ruleId: UUID
    val fingerprint: String
    val sourceModule: String?
    val sourceAction: String?
    val severity: String
    val summary: String
    val detail: String?
    val firstSeenAt: LocalDateTime
    val lastSeenAt: LocalDateTime
    val occurrences: Long
    val resolved: Boolean
    val resolvedAt: LocalDateTime?
    val createdTime: LocalDateTime
    val updatedTime: LocalDateTime
}
