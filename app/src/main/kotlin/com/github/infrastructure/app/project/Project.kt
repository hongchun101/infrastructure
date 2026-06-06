package com.github.infrastructure.app.project

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "projects")
interface Project {
    @Id
    val id: UUID
    val name: String
    val ownerId: UUID
    val createdTime: LocalDateTime
}
