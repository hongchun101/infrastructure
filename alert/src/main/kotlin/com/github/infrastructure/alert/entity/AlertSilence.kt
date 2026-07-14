package com.github.infrastructure.alert.entity

import java.time.LocalDateTime
import java.util.UUID
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "alert_silences")
interface AlertSilence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    val name: String

    val ruleId: UUID?

    @org.babyfish.jimmer.sql.Column(name = "starts_at")
    val startsAt: LocalDateTime

    @org.babyfish.jimmer.sql.Column(name = "ends_at")
    val endsAt: LocalDateTime

    val reason: String?

    val active: Boolean

    val createdBy: String?

    @org.babyfish.jimmer.sql.Column(name = "created_at")
    val createdAt: LocalDateTime

    @org.babyfish.jimmer.sql.Column(name = "updated_at")
    val updatedAt: LocalDateTime
}