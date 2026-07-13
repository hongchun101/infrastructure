package com.github.infrastructure.app.announcement.repository

import com.github.infrastructure.app.announcement.Announcement
import com.github.infrastructure.app.announcement.AnnouncementRead
import com.github.infrastructure.app.announcement.*
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.and
import org.babyfish.jimmer.sql.kt.ast.expression.count
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.babyfish.jimmer.sql.kt.ast.expression.`ilike?`
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.babyfish.jimmer.sql.kt.ast.expression.`valueIn?`
import org.babyfish.jimmer.sql.kt.ast.expression.valueNotIn
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
        val readIds = subQuery(AnnouncementRead::class) {
            where(and(table.userId `eq?` viewerId))
            select(table.announcementId)
        }
        where(
            and(
                table.status `eq?` status,
                or(
                    table.title `ilike?` keyword?.takeIf { it.isNotBlank() }?.let { "%${it.lowercase()}%" },
                    table.summary `ilike?` keyword?.takeIf { it.isNotBlank() }?.let { "%${it.lowercase()}%" },
                ),
                if (unreadOnly) table.id valueNotIn readIds else null,
            ),
        )
        orderBy(table.priority.desc(), table.createdTime.desc())
        select(table)
    }

    fun countReadsByAnnouncementIds(announcementIds: Collection<UUID>): Map<UUID, Long> {
        if (announcementIds.isEmpty()) return emptyMap()
        return sql.createQuery(AnnouncementRead::class) {
            where(and(table.announcementId `valueIn?` announcementIds))
            groupBy(table.announcementId)
            select(table.announcementId, count(table.userId))
        }.execute().associate { it._1 to it._2 }
    }

    fun existsReadByUser(announcementId: UUID, userId: UUID): Boolean =
        sql.createQuery(AnnouncementRead::class) {
            where(
                and(
                    table.announcementId `eq?` announcementId,
                    table.userId `eq?` userId,
                ),
            )
            selectCount()
        }.fetchUnlimitedCount() > 0

    fun findReadAnnouncementIds(userId: UUID, announcementIds: Collection<UUID>): List<UUID> =
        sql.createQuery(AnnouncementRead::class) {
            where(
                and(
                    table.userId `eq?` userId,
                    table.announcementId `valueIn?` announcementIds.takeIf { it.isNotEmpty() },
                ),
            )
            select(table.announcementId)
        }.execute()
}
