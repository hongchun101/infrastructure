package com.github.infrastructure.app.announcement.repository

import com.github.infrastructure.app.announcement.AnnouncementRead
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
class AnnouncementReadRepository(sql: KSqlClient) : AbstractKotlinRepository<AnnouncementRead, UUID>(sql) {
    fun markRead(announcementId: UUID, userId: UUID, readAt: LocalDateTime) {
        save(
            AnnouncementRead {
                id = UUID.nameUUIDFromBytes("$announcementId:$userId".toByteArray())
                this.announcementId = announcementId
                this.userId = userId
                this.readAt = readAt
            },
            SaveMode.UPSERT,
        )
    }
}
