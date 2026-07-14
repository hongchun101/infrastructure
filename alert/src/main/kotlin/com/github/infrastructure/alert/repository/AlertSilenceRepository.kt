package com.github.infrastructure.alert.repository

import com.github.infrastructure.alert.entity.AlertSilence
import com.github.infrastructure.alert.entity.*
import java.time.LocalDateTime
import java.util.UUID
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.and
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.babyfish.jimmer.sql.kt.ast.expression.ge
import org.babyfish.jimmer.sql.kt.ast.expression.le
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.springframework.stereotype.Repository

@Repository
class AlertSilenceRepository(sql: KSqlClient) : AbstractKotlinRepository<AlertSilence, Long>(sql) {

    /**
     * Returns true when at least one active silence covers [at] for [ruleId],
     * either by matching the rule specifically or by being rule-agnostic
     * (rule_id IS NULL).
     */
    fun isSilenced(ruleId: UUID, at: LocalDateTime): Boolean = createQuery {
        where(
            and(
                table.active `eq?` true,
                table.startsAt le at,
                table.endsAt ge at,
                or(
                    table.ruleId `eq?` ruleId,
                    table.ruleId `eq?` null,
                ),
            ),
        )
        select(table.id)
    }.fetchFirst() != null

    fun listActive(at: LocalDateTime): List<AlertSilence> = createQuery {
        where(
            and(
                table.active `eq?` true,
                table.startsAt le at,
                table.endsAt ge at,
            ),
        )
        orderBy(table.startsAt)
        select(table)
    }.execute()

    fun listAll(): List<AlertSilence> = executeQuery {
        orderBy(table.createdAt.desc())
        select(table)
    }
}