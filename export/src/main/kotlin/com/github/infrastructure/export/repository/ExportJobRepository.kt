package com.github.infrastructure.export.repository

import com.github.infrastructure.export.entity.ExportJob
import com.github.infrastructure.export.entity.*
import com.github.infrastructure.export.entity.ExportJobStatus
import java.util.UUID
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.babyfish.jimmer.sql.kt.ast.expression.and
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.isNull
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.babyfish.jimmer.sql.kt.ast.expression.`valueIn?`
import org.springframework.stereotype.Repository

@Repository
class ExportJobRepository(sql: KSqlClient) : AbstractKotlinRepository<ExportJob, Long>(sql) {

    fun findActiveById(id: Long): ExportJob? = createQuery {
        where(
            and(
                table.id `eq?` id,
                table.deletedAt.isNull(),
            ),
        )
        select(table)
    }.fetchOneOrNull()

    fun listPending(limit: Int): List<ExportJob> = createQuery {
        where(
            and(
                table.status `eq?` ExportJobStatus.PENDING,
                table.deletedAt.isNull(),
            ),
        )
        orderBy(table.createdAt)
        select(table).limit(limit.coerceAtLeast(1))
    }.execute()

    fun listByOwner(owner: UUID, limit: Int): List<ExportJob> = createQuery {
        where(
            and(
                table.ownerUserId `eq?` owner,
                table.deletedAt.isNull(),
            ),
        )
        orderBy(table.createdAt.desc())
        select(table).limit(limit.coerceIn(1, 200))
    }.execute()

    fun listAll(): List<ExportJob> = createQuery {
        where(table.deletedAt.isNull())
        orderBy(table.createdAt.desc())
        select(table).limit(200)
    }.execute()

    fun countActiveByOwner(owner: UUID): Long = createQuery {
        where(
            and(
                table.ownerUserId `eq?` owner,
                or(
                    table.status `eq?` ExportJobStatus.PENDING,
                    table.status `eq?` ExportJobStatus.RUNNING,
                ),
                table.deletedAt.isNull(),
            ),
        )
        selectCount()
    }.fetchUnlimitedCount()
}