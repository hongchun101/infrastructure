package com.github.infrastructure.app.audit.login.repository

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
class LoginAuditRepository(sql: KSqlClient) : AbstractKotlinRepository<LoginAuditEntry, UUID>(sql) {

    fun findPage(
        accountType: String?,
        outcome: String?,
        principal: String?,
        accountId: UUID?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        pageIndex: Int,
        pageSize: Int,
    ): Page<LoginAuditEntry> = createQuery {
        where(
            and(
                table.accountType `eq?` accountType?.takeIf { it.isNotBlank() },
                table.outcome `eq?` outcome?.takeIf { it.isNotBlank() },
                table.principal `eq?` principal?.takeIf { it.isNotBlank() },
                table.accountId `eq?` accountId,
                table.createdTime `ge?` startTime,
                table.createdTime `lt?` endTime,
            ),
        )
        orderBy(table.createdTime.desc(), table.id.desc())
        select(table)
    }.fetchPage(pageIndex, pageSize)
}
