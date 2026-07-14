package com.github.infrastructure.alert.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "alert_rules")
interface AlertRule {
    @Id
    val id: UUID
    val code: String
    val name: String
    val description: String?
    val ruleType: String
    val severity: String
    val enabled: Boolean
    val sourceModule: String?
    val sourceAction: String?
    val config: String
    val channels: String
    val createdTime: LocalDateTime
    val updatedTime: LocalDateTime
}
