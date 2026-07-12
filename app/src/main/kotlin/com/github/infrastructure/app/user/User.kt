package com.github.infrastructure.app.user

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "users")
interface User {
    @Id
    val id: UUID
    val username: String
    val email: String?
    val phone: String?
    val passwordHash: String
    val displayName: String
    val enabled: Boolean
    val createdTime: LocalDateTime
}
