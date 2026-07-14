package com.github.infrastructure.notification.repository

import com.github.infrastructure.notification.entity.UserNotification
import com.github.infrastructure.notification.entity.*
import java.util.UUID
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.and
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.isNull
import org.springframework.stereotype.Repository

@Repository
class UserNotificationRepository(sql: KSqlClient) : AbstractKotlinRepository<UserNotification, Long>(sql) {

    fun listInbox(
        recipient: UUID,
        onlyUnread: Boolean,
        includeArchived: Boolean,
        limit: Int,
    ): List<UserNotification> {
        val effectiveLimit = limit.coerceIn(1, 200)
        return createQuery {
            where(
                and(
                    table.recipientUserId `eq?` recipient,
                    if (onlyUnread) table.readAt.isNull() else null,
                    if (includeArchived) null else table.archivedAt.isNull(),
                ),
            )
            orderBy(table.createdAt.desc())
            select(table).limit(effectiveLimit)
        }.execute()
    }

    fun findByIdAndRecipient(id: Long, recipient: UUID): UserNotification? = createQuery {
        where(
            and(
                table.id `eq?` id,
                table.recipientUserId `eq?` recipient,
            ),
        )
        select(table)
    }.fetchOneOrNull()

    fun countUnread(recipient: UUID): Long = createQuery {
        where(
            and(
                table.recipientUserId `eq?` recipient,
                table.readAt.isNull(),
                table.archivedAt.isNull(),
            ),
        )
        selectCount()
    }.fetchUnlimitedCount()
}