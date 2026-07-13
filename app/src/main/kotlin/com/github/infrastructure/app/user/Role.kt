package com.github.infrastructure.app.user

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.OneToMany
import org.babyfish.jimmer.sql.Table
import java.util.UUID

@Entity
@Table(name = "roles")
interface Role {
    @Id
    val id: UUID
    val code: String
    val name: String

    @OneToMany(mappedBy = "role")
    val userRoles: List<UserRole>

    @OneToMany(mappedBy = "role")
    val rolePermissions: List<RolePermission>
}