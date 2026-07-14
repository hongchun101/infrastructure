package com.github.infrastructure.scheduler.repository

import com.github.infrastructure.scheduler.entity.JobDefinition
import com.github.infrastructure.scheduler.entity.*
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.babyfish.jimmer.sql.kt.ast.expression.and
import org.babyfish.jimmer.sql.kt.ast.expression.le
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.springframework.stereotype.Repository

@Repository
class JobDefinitionRepository(sql: KSqlClient) : AbstractKotlinRepository<JobDefinition, Long>(sql) {

    fun findByCode(code: String): JobDefinition? = createQuery {
        where(table.code `eq?` code)
        select(table)
    }.fetchOneOrNull()

    fun listEnabled(): List<JobDefinition> = createQuery {
        where(table.enabled `eq?` true)
        orderBy(table.code)
        select(table)
    }.execute()

    fun listAll(): List<JobDefinition> = createQuery {
        orderBy(table.code)
        select(table)
    }.execute()
}