package com.github.infrastructure.app.user.repository

import com.github.infrastructure.app.user.entity.Permission
import com.github.infrastructure.app.user.entity.*
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.babyfish.jimmer.sql.kt.ast.expression.`valueIn?`
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class PermissionRepository(sql: KSqlClient) : AbstractKotlinRepository<Permission, UUID>(sql) {

    fun findByCode(code: String): Permission? = createQuery {
        where(table.code `eq?` code)
        select(table)
    }.fetchOneOrNull()

    fun findAllOrderedByCode(): List<Permission> = executeQuery {
        orderBy(table.code)
        select(table)
    }

    fun findExistingIds(ids: Collection<UUID>): List<UUID> = createQuery {
        where(table.id `valueIn?` ids.takeIf { it.isNotEmpty() })
        select(table.id)
    }.execute()
}
