package com.github.infrastructure.alert.repository

import com.github.infrastructure.alert.entity.AlertRule
import com.github.infrastructure.alert.entity.*
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.and
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.babyfish.jimmer.sql.kt.ast.expression.ne
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class AlertRuleRepository(sql: KSqlClient) : AbstractKotlinRepository<AlertRule, UUID>(sql) {
    fun findByCode(code: String): AlertRule? = createQuery {
        where(table.code `eq?` code)
        select(table)
    }.fetchOneOrNull()

    fun findAllEnabled(): List<AlertRule> = executeQuery {
        where(table.enabled `eq?` true)
        orderBy(table.createdTime)
        select(table)
    }

    fun findByCodeExcluding(code: String, excludeId: UUID): AlertRule? = createQuery {
        where(
            and(
                table.code `eq?` code,
                table.id ne excludeId,
            ),
        )
        select(table)
    }.fetchOneOrNull()
}
