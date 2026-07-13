package com.github.infrastructure.app.announcement

import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.coalesce
import org.babyfish.jimmer.sql.kt.ast.expression.count
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.notIn
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.babyfish.jimmer.sql.kt.ast.expression.value
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class AnnouncementRepository(sql: KSqlClient) : AbstractKotlinRepository<Announcement, UUID>(sql) {
    fun list(
        status: String?,
        keyword: String?,
        viewerId: UUID?,
        unreadOnly: Boolean,
    ): List<Announcement> = executeQuery {
        if (status != null) where(table.status eq status)
        if (!keyword.isNullOrBlank()) {
            val pattern = "%${keyword.lowercase()}%"
            where((table.title ilike pattern) or (coalesce(table.summary, value("")) ilike pattern))
        }
        if (unreadOnly && viewerId != null) {
            where(
                table.id notIn subQuery(AnnouncementRead::class) {
                    where(table.userId eq viewerId)
                    select(table.announcementId)
                },
            )
        }
        orderBy(table.priority.desc(), table.createdTime.desc())
        select(table)
    }

    fun countReadsByAnnouncementIds(announcementIds: Collection<UUID>): Map<UUID, Int> {
        if (announcementIds.isEmpty()) return emptyMap()
        return sql.createQuery(AnnouncementRead::class) {
            where(table.announcementId valueIn announcementIds)
            groupBy(table.announcementId)
            select(table.announcementId, count(table.userId))
        }.execute().associate { it._1 to it._2 }
    }

    fun existsReadByUser(announcementId: UUID, userId: UUID): Boolean =
        sql.createQuery(AnnouncementRead::class) {
            where(table.announcementId eq announcementId)
            where(table.userId eq userId)
            selectCount()
        }.fetchUnlimitedCount() > 0

    fun findReadAnnouncementIds(userId: UUID, announcementIds: Collection<UUID>): List<UUID> =
        sql.createQuery(AnnouncementRead::class) {
            where(table.userId eq userId)
            where(table.announcementId valueIn announcementIds)
            select(table.announcementId)
        }.execute()
}