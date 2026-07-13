package com.github.infrastructure.app.user.repository

import com.github.infrastructure.app.user.BackendAccount
import com.github.infrastructure.app.user.*
import com.github.infrastructure.security.auth.LoginMode
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class BackendAccountRepository(sql: KSqlClient) : AbstractKotlinRepository<BackendAccount, UUID>(sql) {
    fun findForLogin(mode: LoginMode, principal: String): BackendAccount? = createQuery {
        when (mode) {
            LoginMode.USERNAME -> where(table.username eq principal)
            LoginMode.EMAIL -> where(table.email eq principal)
            LoginMode.PHONE -> where(table.phone eq principal)
        }
        select(table)
    }.fetchOneOrNull()
}
