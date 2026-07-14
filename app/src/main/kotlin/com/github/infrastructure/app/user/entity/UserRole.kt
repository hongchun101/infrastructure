package com.github.infrastructure.app.user.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.Table
import java.util.UUID

@Entity
@Table(name = "user_roles")
interface UserRole {
    @org.babyfish.jimmer.sql.Id
    val id: UUID
    @Key
    @ManyToOne
    val user: User

    @IdView("user")
    val userId: UUID

    @Key
    @ManyToOne
    val role: Role

    @IdView("role")
    val roleId: UUID
}
