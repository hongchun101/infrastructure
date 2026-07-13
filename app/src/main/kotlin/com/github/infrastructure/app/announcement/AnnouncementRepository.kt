package com.github.infrastructure.app.announcement

import org.babyfish.jimmer.spring.repository.JRepository
import org.babyfish.jimmer.sql.kt.ast.expression.coalesce
import org.babyfish.jimmer.sql.kt.ast.expression.count
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.notIn
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.babyfish.jimmer.sql.kt.ast.expression.value
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.babyfish.jimmer.sql.kt.toKSqlClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AnnouncementRepository : JRepository<Announcement, UUID> {
    fun list(
        status: String?,
        keyword: String?,
        viewerId: UUID?,
        unreadOnly: Boolean,
    ): List<Announcement> = toKSqlClient(sql()).createQuery(Announcement::class) {
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
    }.execute()

    fun countReadsByAnnouncementIds(announcementIds: Collection<UUID>): Map<UUID, Int> {
        if (announcementIds.isEmpty()) return emptyMap()
        return toKSqlClient(sql()).createQuery(AnnouncementRead::class) {
            where(table.announcementId valueIn announcementIds)
            groupBy(table.announcementId)
            select(table.announcementId, count(table.userId))
        }.execute().associate { it._1 to it._2 }
    }

    fun findReadAnnouncementIds(userId: UUID, announcementIds: Collection<UUID>): List<UUID> =
        toKSqlClient(sql()).createQuery(AnnouncementRead::class) {
            where(table.userId eq userId)
            where(table.announcementId valueIn announcementIds)
            select(table.announcementId)
        }.execute()
}