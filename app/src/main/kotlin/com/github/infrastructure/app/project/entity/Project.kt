package com.github.infrastructure.app.project.entity

import com.github.infrastructure.app.user.entity.User
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.JoinColumn
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "projects")
interface Project {
    @Id
    val id: UUID
    val name: String

    @ManyToOne
    @JoinColumn(name = "owner_id")
    val owner: User

    @IdView("owner")
    val ownerId: UUID

    val createdTime: LocalDateTime
}
