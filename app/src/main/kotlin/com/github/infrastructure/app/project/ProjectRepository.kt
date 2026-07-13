package com.github.infrastructure.app.project

import org.babyfish.jimmer.spring.repository.JRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.toKSqlClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProjectRepository : JRepository<Project, UUID> {
    fun findByOwnerId(ownerId: UUID): List<Project> =
        toKSqlClient(sql()).createQuery(Project::class) {
            where(table.ownerId eq ownerId)
            orderBy(table.createdTime.desc())
            select(table)
        }.execute()

    fun findByIdAndOwnerId(id: UUID, ownerId: UUID): Project? =
        toKSqlClient(sql()).createQuery(Project::class) {
            where(table.id eq id)
            where(table.ownerId eq ownerId)
            select(table)
        }.fetchOneOrNull()
}