package com.github.infrastructure.app.announcement

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.mutation.SaveMode
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
class AnnouncementReadRepository(
    private val sql: KSqlClient,
) {
    fun markRead(announcementId: UUID, userId: UUID, readAt: LocalDateTime) {
        sql.save(
            AnnouncementRead {
                this.announcementId = announcementId
                this.userId = userId
                this.readAt = readAt
            },
            SaveMode.UPSERT,
        )
    }
}