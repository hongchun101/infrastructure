package com.github.infrastructure.filestore.repository

import com.github.infrastructure.filestore.entity.FileObject
import com.github.infrastructure.filestore.entity.*
import java.util.UUID
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.babyfish.jimmer.sql.kt.ast.expression.and
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.isNull
import org.springframework.stereotype.Repository

@Repository
class FileObjectRepository(sql: KSqlClient) : AbstractKotlinRepository<FileObject, Long>(sql) {

    fun findActiveById(id: Long): FileObject? = createQuery {
        where(
            and(
                table.id `eq?` id,
                table.deletedAt.isNull(),
            ),
        )
        select(table)
    }.fetchOneOrNull()

    fun listByOwner(
        owner: UUID,
        limit: Int,
    ): List<FileObject> = createQuery {
        where(
            and(
                table.ownerUserId `eq?` owner,
                table.deletedAt.isNull(),
            ),
        )
        orderBy(table.createdAt.desc())
        select(table).limit(limit.coerceIn(1, 200))
    }.execute()
}