package com.github.infrastructure.app.user

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class UserRoleRepository(
    private val sql: KSqlClient,
) {
    fun findRoleCodesByUserId(userId: UUID): List<String> =
        sql.createQuery(UserRole::class) {
            where(table.userId eq userId)
            select(table.role.code)
        }.execute().sorted()

    fun findRoleCodesByUserIds(userIds: Collection<UUID>): List<String> =
        sql.createQuery(UserRole::class) {
            where(table.userId valueIn userIds)
            select(table.role.code)
        }.execute().distinct().sorted()
}