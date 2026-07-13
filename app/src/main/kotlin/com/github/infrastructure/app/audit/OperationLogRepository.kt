package com.github.infrastructure.app.audit

import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ge
import org.babyfish.jimmer.sql.kt.ast.expression.lt
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
        if (!module.isNullOrBlank()) where(table.module eq module)
        if (!action.isNullOrBlank()) where(table.action eq action)
        if (userId != null) where(table.userId eq userId)
        if (success != null) where(table.success eq success)
        if (startTime != null) where(table.createdTime ge startTime)
        if (endTime != null) where(table.createdTime lt endTime)
        orderBy(table.createdTime.desc(), table.id.desc())
        select(table)
    }.fetchPage(pageIndex, pageSize)
}