package com.github.infrastructure.alert.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "alert_notifications")
interface AlertNotification {
    @Id
    val id: UUID

    @Key
    val eventId: UUID
    val channel: String
    val target: String
    val status: String
    val httpStatus: Int?
    val errorMessage: String?
    val payload: String?
    val sentAt: LocalDateTime
    val createdTime: LocalDateTime
}
