package com.github.infrastructure.app.user

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class RolePermissionRepository(
    private val sql: KSqlClient,
) {
    fun findPermissionCodesByRoleId(roleId: UUID): List<String> =
        sql.createQuery(RolePermission::class) {
            where(table.roleId eq roleId)
            select(table.permission.code)
        }.execute().sorted()

    fun findPermissionCodesByRoleCodes(roleCodes: Collection<String>): List<String> =
        sql.createQuery(RolePermission::class) {
            where(table.role.code valueIn roleCodes)
            select(table.permission.code)
        }.execute().distinct().sorted()
}