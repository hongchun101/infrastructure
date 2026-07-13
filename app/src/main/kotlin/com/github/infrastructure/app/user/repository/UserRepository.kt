package com.github.infrastructure.app.user.repository

import com.github.infrastructure.app.user.User
import com.github.infrastructure.app.user.*
import com.github.infrastructure.security.auth.LoginMode
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class UserRepository(sql: KSqlClient) : AbstractKotlinRepository<User, UUID>(sql) {
    fun findForLogin(mode: LoginMode, principal: String): User? = when (mode) {
        LoginMode.USERNAME -> findByUsername(principal)
        LoginMode.EMAIL -> findByEmail(principal)
        LoginMode.PHONE -> findByPhone(principal)
    }

    fun findByUsername(username: String): User? = createQuery {
        where(table.username `eq?` username)
        select(table)
    }.fetchOneOrNull()

    fun findByEmail(email: String): User? = createQuery {
        where(table.email `eq?` email)
        select(table)
    }.fetchOneOrNull()

    fun findByPhone(phone: String): User? = createQuery {
        where(table.phone `eq?` phone)
        select(table)
    }.fetchOneOrNull()
}
