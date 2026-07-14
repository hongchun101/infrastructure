package com.github.infrastructure.alert.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "alert_window_counters")
interface AlertWindowCounter {
    @Id
    val id: UUID
    val ruleId: UUID
    val bucketMinute: LocalDateTime
    val totalCount: Long
    val failedCount: Long
    val createdTime: LocalDateTime
    val updatedTime: LocalDateTime
}
