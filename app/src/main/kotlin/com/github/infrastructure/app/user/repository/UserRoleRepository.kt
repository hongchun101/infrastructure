package com.github.infrastructure.app.user.repository

import com.github.infrastructure.app.user.entity.UserRole
import com.github.infrastructure.app.user.entity.*
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.babyfish.jimmer.sql.kt.ast.expression.`valueIn?`
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class UserRoleRepository(sql: KSqlClient) : AbstractKotlinRepository<UserRole, UUID>(sql) {
    fun findRoleCodesByUserId(userId: UUID): List<String> = executeQuery {
        where(table.userId `eq?` userId)
        select(table.role.code)
    }.sorted()

    fun findRoleCodesByUserIds(userIds: Collection<UUID>): List<String> = executeQuery {
        where(table.userId `valueIn?` userIds.takeIf { it.isNotEmpty() })
        select(table.role.code)
    }.distinct().sorted()
}
