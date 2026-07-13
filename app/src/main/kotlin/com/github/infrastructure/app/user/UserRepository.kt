package com.github.infrastructure.app.user

import org.babyfish.jimmer.spring.repository.JRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.toKSqlClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : JRepository<User, UUID> {
    fun findByUsername(username: String): User? =
        toKSqlClient(sql()).createQuery(User::class) {
            where(table.username eq username)
            select(table)
        }.fetchOneOrNull()

    fun findByEmail(email: String): User? =
        toKSqlClient(sql()).createQuery(User::class) {
            where(table.email eq email)
            select(table)
        }.fetchOneOrNull()

    fun findByPhone(phone: String): User? =
        toKSqlClient(sql()).createQuery(User::class) {
            where(table.phone eq phone)
            select(table)
        }.fetchOneOrNull()
}