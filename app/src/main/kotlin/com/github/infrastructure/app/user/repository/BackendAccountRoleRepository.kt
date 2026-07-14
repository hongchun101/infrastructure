package com.github.infrastructure.app.user.repository

import com.github.infrastructure.app.user.entity.BackendAccountRole
import com.github.infrastructure.app.user.entity.*
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class BackendAccountRoleRepository(sql: KSqlClient) : AbstractKotlinRepository<BackendAccountRole, UUID>(sql) {
    fun findRoleCodesByAccountId(accountId: UUID): List<String> = executeQuery {
        where(table.accountId `eq?` accountId)
        select(table.role.code)
    }.sorted()
}
