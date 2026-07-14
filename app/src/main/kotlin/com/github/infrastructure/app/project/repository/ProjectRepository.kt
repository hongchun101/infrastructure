package com.github.infrastructure.app.project.repository

import com.github.infrastructure.app.project.entity.Project
import com.github.infrastructure.app.project.entity.*
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.and
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class ProjectRepository(sql: KSqlClient) : AbstractKotlinRepository<Project, UUID>(sql) {
    fun findByOwnerId(ownerId: UUID): List<Project> = executeQuery {
        where(and(table.ownerId `eq?` ownerId))
        orderBy(table.createdTime.desc())
        select(table)
    }

    fun findByIdAndOwnerId(id: UUID, ownerId: UUID): Project? = createQuery {
        where(
            and(
                table.id `eq?` id,
                table.ownerId `eq?` ownerId,
            ),
        )
        select(table)
    }.fetchOneOrNull()
}
