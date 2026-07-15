package com.github.infrastructure.app.user.repository

import com.github.infrastructure.app.user.entity.RolePermission
import com.github.infrastructure.app.user.entity.*
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.babyfish.jimmer.sql.kt.ast.expression.`valueIn?`
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class RolePermissionRepository(sql: KSqlClient) : AbstractKotlinRepository<RolePermission, UUID>(sql) {
    fun findPermissionCodesByRoleId(roleId: UUID): List<String> = executeQuery {
        where(table.roleId `eq?` roleId)
        select(table.role.code)
    }.sorted()

    fun findPermissionCodesByRoleCodes(roleCodes: Collection<String>): List<String> = executeQuery {
        where(table.role.code `valueIn?` roleCodes.takeIf { it.isNotEmpty() })
        select(table.permission.code)
    }.distinct().sorted()

    fun findPermissionIdsByRoleId(roleId: UUID): List<UUID> = executeQuery {
        where(table.roleId `eq?` roleId)
        select(table.permissionId)
    }

    fun existsByPermissionId(permissionId: UUID): Boolean = createQuery {
        where(table.permissionId `eq?` permissionId)
        selectCount()
    }.fetchUnlimitedCount() > 0

    fun deleteByRoleId(roleId: UUID): Int = executeDelete {
        where(table.roleId `eq?` roleId)
    }
}
