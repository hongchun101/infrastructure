package com.github.infrastructure.app.user

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.Table
import java.util.UUID

@Entity
@Table(name = "role_permissions")
interface RolePermission {
    @Key
    @ManyToOne
    val role: Role

    @IdView("role")
    val roleId: UUID

    @Key
    @ManyToOne
    val permission: Permission

    @IdView("permission")
    val permissionId: UUID
}