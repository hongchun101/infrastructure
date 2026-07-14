package com.github.infrastructure.alert.repository

import com.github.infrastructure.alert.entity.AlertWindowCounter
import com.github.infrastructure.alert.entity.*
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.babyfish.jimmer.sql.kt.ast.expression.`ge?`
import org.babyfish.jimmer.sql.kt.ast.expression.`le?`
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
class AlertWindowCounterRepository(
    sql: KSqlClient,
) : AbstractKotlinRepository<AlertWindowCounter, UUID>(sql) {

    fun findByRuleAndBucket(ruleId: UUID, bucketMinute: LocalDateTime): AlertWindowCounter? =
        createQuery {
            where(
                table.ruleId `eq?` ruleId,
                table.bucketMinute `eq?` bucketMinute,
            )
            select(table)
        }.fetchOneOrNull()

    fun summarize(
        ruleId: UUID,
        fromExclusive: LocalDateTime,
        toInclusive: LocalDateTime,
    ): WindowSummary {
        val rows = executeQuery {
            where(
                table.ruleId `eq?` ruleId,
                table.bucketMinute `ge?` fromExclusive,
                table.bucketMinute `le?` toInclusive,
            )
            select(table)
        }
        val total = rows.sumOf { it.totalCount }
        val failed = rows.sumOf { it.failedCount }
        return WindowSummary(total = total, failed = failed)
    }

    data class WindowSummary(val total: Long, val failed: Long) {
        val rate: Double get() = if (total == 0L) 0.0 else failed.toDouble() / total.toDouble()
    }
}
