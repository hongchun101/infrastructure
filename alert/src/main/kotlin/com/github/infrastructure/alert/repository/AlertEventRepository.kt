package com.github.infrastructure.alert.repository

import com.github.infrastructure.alert.entity.AlertEvent
import com.github.infrastructure.alert.entity.*
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.and
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.babyfish.jimmer.sql.kt.ast.expression.`ge?`
import org.babyfish.jimmer.sql.kt.ast.expression.`lt?`
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
class AlertEventRepository(sql: KSqlClient) : AbstractKotlinRepository<AlertEvent, UUID>(sql) {
    fun findByFingerprint(fingerprint: String): AlertEvent? = createQuery {
        where(table.fingerprint `eq?` fingerprint)
        select(table)
    }.fetchOneOrNull()

    fun findById(id: UUID): AlertEvent? = createQuery {
        where(table.id `eq?` id)
        select(table)
    }.fetchOneOrNull()

    fun findPage(
        ruleId: UUID?,
        severity: String?,
        resolved: Boolean?,
        pageIndex: Int,
        pageSize: Int,
    ): Page<AlertEvent> = createQuery {
        where(
            and(
                table.ruleId `eq?` ruleId,
                table.severity `eq?` severity,
                if (resolved != null) table.resolved `eq?` resolved else null,
            ),
        )
        orderBy(table.lastSeenAt.desc(), table.id.desc())
        select(table)
    }.fetchPage(pageIndex, pageSize)

    fun findRecentlyActive(fromTime: LocalDateTime): List<AlertEvent> = executeQuery {
        where(table.lastSeenAt `ge?` fromTime)
        orderBy(table.lastSeenAt.desc())
        select(table)
    }

    fun findIdleUnresolved(before: LocalDateTime): List<AlertEvent> = executeQuery {
        where(
            and(
                table.resolved `eq?` false,
                table.lastSeenAt `lt?` before,
            ),
        )
        orderBy(table.lastSeenAt)
        select(table)
    }
}
