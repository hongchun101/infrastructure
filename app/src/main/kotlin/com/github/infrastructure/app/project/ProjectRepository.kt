package com.github.infrastructure.app.project

import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class ProjectRepository(sql: KSqlClient) : AbstractKotlinRepository<Project, UUID>(sql) {
    fun findByOwnerId(ownerId: UUID): List<Project> = executeQuery {
        where(table.ownerId eq ownerId)
        orderBy(table.createdTime.desc())
        select(table)
    }

    fun findByIdAndOwnerId(id: UUID, ownerId: UUID): Project? = createQuery {
        where(table.id eq id)
        where(table.ownerId eq ownerId)
        select(table)
    }.fetchOneOrNull()
}