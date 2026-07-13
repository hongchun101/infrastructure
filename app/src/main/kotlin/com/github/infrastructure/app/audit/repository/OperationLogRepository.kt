package com.github.infrastructure.app.audit.repository

import com.github.infrastructure.app.audit.OperationLogEntry
import com.github.infrastructure.app.audit.*
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
class OperationLogRepository(sql: KSqlClient) : AbstractKotlinRepository<OperationLogEntry, UUID>(sql) {
    fun findPage(
        module: String?,
        action: String?,
        userId: UUID?,
        success: Boolean?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        pageIndex: Int,
        pageSize: Int,
    ): Page<OperationLogEntry> = createQuery {
        where(
            and(
                table.module `eq?` module?.takeIf { it.isNotBlank() },
                table.action `eq?` action?.takeIf { it.isNotBlank() },
                table.userId `eq?` userId,
                table.success `eq?` success,
                table.createdTime `ge?` startTime,
                table.createdTime `lt?` endTime,
            ),
        )
        orderBy(table.createdTime.desc(), table.id.desc())
        select(table)
    }.fetchPage(pageIndex, pageSize)
}
