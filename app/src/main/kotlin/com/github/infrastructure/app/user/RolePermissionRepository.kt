package com.github.infrastructure.app.user

import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class RolePermissionRepository(sql: KSqlClient) : AbstractKotlinRepository<RolePermission, UUID>(sql) {
    fun findPermissionCodesByRoleId(roleId: UUID): List<String> = executeQuery {
        where(table.roleId eq roleId)
        select(table.permission.code)
    }.sorted()

    fun findPermissionCodesByRoleCodes(roleCodes: Collection<String>): List<String> = executeQuery {
        where(table.role.code valueIn roleCodes)
        select(table.permission.code)
    }.distinct().sorted()
}