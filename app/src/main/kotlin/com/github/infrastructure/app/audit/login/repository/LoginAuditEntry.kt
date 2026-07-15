package com.github.infrastructure.app.audit.login.repository

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.LocalDateTime
import java.util.UUID

enum class LoginOutcome {
    SUCCESS,
    FAILURE,
    LOGOUT,
}

@Entity
@Table(name = "login_audits")
interface LoginAuditEntry {
    @Id
    val id: UUID

    val accountType: String

    val loginMode: String?

    val principal: String?

    val accountId: UUID?

    val username: String?

    val outcome: String

    val failureReason: String?

    val clientIp: String?

    val userAgent: String?

    val traceId: String?

    val createdTime: LocalDateTime
}
