package com.github.infrastructure.app.user.repository

import com.github.infrastructure.app.user.entity.Role
import com.github.infrastructure.app.user.entity.*
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class RoleRepository(sql: KSqlClient) : AbstractKotlinRepository<Role, UUID>(sql) {

    fun findByCode(code: String): Role? = createQuery {
        where(table.code `eq?` code)
        select(table)
    }.fetchOneOrNull()

    fun findAllOrderedByCode(): List<Role> = executeQuery {
        orderBy(table.code)
        select(table)
    }

    fun findByIdWithPermissions(id: UUID): Role? = createQuery {
        where(table.id `eq?` id)
        select(table.fetch(fetcherWithPermissions))
    }.fetchOneOrNull()

    companion object {
        private val fetcherWithPermissions = newFetcher(Role::class).by {
            rolePermissions {
                permission()
            }
        }
    }
}
