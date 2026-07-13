package com.github.infrastructure.app.audit

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "operation_logs")
interface OperationLogEntry {
    @Id
    val id: UUID
    val traceId: String?
    val userId: UUID?
    val username: String?
    val module: String
    val action: String
    val description: String?
    val method: String
    val path: String
    val queryString: String?
    val responseStatus: Int?
    val errorMessage: String?
    val clientIp: String?
    val userAgent: String?
    val durationMs: Long
    val success: Boolean
    val createdTime: LocalDateTime
}