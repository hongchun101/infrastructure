package com.github.infrastructure.scheduler.repository

import com.github.infrastructure.scheduler.entity.JobExecution
import com.github.infrastructure.scheduler.entity.*
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.babyfish.jimmer.sql.kt.ast.expression.and
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.le
import org.springframework.stereotype.Repository

@Repository
class JobExecutionRepository(sql: KSqlClient) : AbstractKotlinRepository<JobExecution, Long>(sql) {

    fun listPending(limit: Int): List<JobExecution> = createQuery {
        where(table.status `eq?` "PENDING")
        orderBy(table.scheduledAt)
        select(table).limit(limit.coerceAtLeast(1))
    }.execute()

    fun listByJob(jobId: Long, limit: Int): List<JobExecution> = createQuery {
        where(table.jobId `eq?` jobId)
        orderBy(table.startedAt.desc())
        select(table).limit(limit.coerceIn(1, 200))
    }.execute()

    fun countRecentFailures(jobId: Long): Long = createQuery {
        where(
            and(
                table.jobId `eq?` jobId,
                table.status `eq?` "FAILED",
            ),
        )
        select(table.id)
    }.execute().size.toLong()
}