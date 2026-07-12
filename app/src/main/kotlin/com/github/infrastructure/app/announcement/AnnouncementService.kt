package com.github.infrastructure.app.announcement

import com.github.infrastructure.core.web.exception.BusinessException
import com.github.infrastructure.security.context.AuthenticatedUser
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class AnnouncementService(
    private val jdbcClient: JdbcClient,
    private val clock: Clock,
) {
    @Transactional
    fun create(request: CreateAnnouncementRequest, user: AuthenticatedUser): AnnouncementResponse {
        val now = LocalDateTime.now(clock)
        val id = UUID.randomUUID()
        jdbcClient.sql(
            """
            insert into announcements
                (id, title, summary, content, status, priority,
                 published_at, publish_at, created_by, updated_by, created_time, updated_time)
            values
                (:id, :title, :summary, :content, :status, :priority,
                 null, null, :createdBy, :updatedBy, :createdTime, :updatedTime)
            """.trimIndent(),
        )
            .param("id", id)
            .param("title", request.title)
            .param("summary", request.summary)
            .param("content", request.content)
            .param("status", AnnouncementStatus.DRAFT.name)
            .param("priority", request.priority)
            .param("createdBy", user.id)
            .param("updatedBy", user.id)
            .param("createdTime", now)
            .param("updatedTime", now)
            .update()
        return getOrThrow(id, viewerId = user.id)
    }

    fun list(
        status: AnnouncementStatus?,
        keyword: String?,
        unreadOnly: Boolean,
        user: AuthenticatedUser,
    ): List<AnnouncementResponse> {
        val sql = StringBuilder(
            """
            select a.id, a.title, a.summary, a.content, a.status, a.priority,
                   a.published_at, a.publish_at,
                   (select count(*) from announcement_reads r where r.announcement_id = a.id) as read_count,
                   case when exists (
                        select 1 from announcement_reads r
                        where r.announcement_id = a.id and r.user_id = :viewer
                   ) then 1 else 0 end as read_by_me,
                   a.created_by, a.updated_by, a.created_time, a.updated_time
            from announcements a
            """.trimIndent(),
        )
        val conditions = mutableListOf<String>()
        val params = mutableMapOf<String, Any?>()
        params["viewer"] = user.id
        status?.let {
            conditions += "a.status = :status"
            params["status"] = it.name
        }
        if (!keyword.isNullOrBlank()) {
            conditions += "(lower(a.title) like :keyword or lower(coalesce(a.summary, '')) like :keyword)"
            params["keyword"] = "%${keyword.lowercase()}%"
        }
        if (unreadOnly) {
            conditions += "not exists (select 1 from announcement_reads r where r.announcement_id = a.id and r.user_id = :viewer)"
        }
        if (conditions.isNotEmpty()) {
            sql.append(" where ").append(conditions.joinToString(" and "))
        }
        sql.append(" order by a.priority desc, a.created_time desc")
        var query = jdbcClient.sql(sql.toString())
        params.forEach { (k, v) -> query = query.param(k, v) }
        return query.query(::mapAnnouncement).list()
    }

    fun get(id: UUID, user: AuthenticatedUser): AnnouncementResponse =
        getOrThrow(id, viewerId = user.id)

    @Transactional
    fun update(id: UUID, request: UpdateAnnouncementRequest, user: AuthenticatedUser): AnnouncementResponse {
        val current = getOrThrow(id, viewerId = user.id)
        if (current.status == AnnouncementStatus.ARCHIVED) {
            throw BusinessException(
                HttpStatus.CONFLICT.value(),
                "archived announcement cannot be edited",
                HttpStatus.CONFLICT,
            )
        }
        val now = LocalDateTime.now(clock)
        val updated = jdbcClient.sql(
            """
            update announcements
            set title = :title,
                summary = :summary,
                content = :content,
                priority = :priority,
                updated_by = :updatedBy,
                updated_time = :updatedTime
            where id = :id
            """.trimIndent(),
        )
            .param("id", id)
            .param("title", request.title)
            .param("summary", request.summary)
            .param("content", request.content)
            .param("priority", request.priority)
            .param("updatedBy", user.id)
            .param("updatedTime", now)
            .update()
        if (updated == 0) {
            throw notFound()
        }
        return getOrThrow(id, viewerId = user.id)
    }

    @Transactional
    fun publish(id: UUID, user: AuthenticatedUser): AnnouncementResponse {
        val current = getOrThrow(id, viewerId = user.id)
        if (current.status == AnnouncementStatus.PUBLISHED) {
            return current
        }
        if (current.status == AnnouncementStatus.ARCHIVED) {
            throw BusinessException(
                HttpStatus.CONFLICT.value(),
                "archived announcement cannot be republished",
                HttpStatus.CONFLICT,
            )
        }
        val now = LocalDateTime.now(clock)
        if (current.publishAt != null && current.publishAt.isAfter(now)) {
            throw BusinessException(
                HttpStatus.CONFLICT.value(),
                "scheduled publish time has not been reached",
                HttpStatus.CONFLICT,
            )
        }
        val publishedAt = current.publishAt ?: now
        val updated = jdbcClient.sql(
            """
            update announcements
            set status = :status,
                published_at = :publishedAt,
                updated_by = :updatedBy,
                updated_time = :updatedTime
            where id = :id
            """.trimIndent(),
        )
            .param("id", id)
            .param("status", AnnouncementStatus.PUBLISHED.name)
            .param("publishedAt", publishedAt)
            .param("updatedBy", user.id)
            .param("updatedTime", now)
            .update()
        if (updated == 0) {
            throw notFound()
        }
        return getOrThrow(id, viewerId = user.id)
    }

    @Transactional
    fun archive(id: UUID, user: AuthenticatedUser): AnnouncementResponse {
        val current = getOrThrow(id, viewerId = user.id)
        if (current.status == AnnouncementStatus.ARCHIVED) {
            return current
        }
        val now = LocalDateTime.now(clock)
        val updated = jdbcClient.sql(
            """
            update announcements
            set status = :status,
                updated_by = :updatedBy,
                updated_time = :updatedTime
            where id = :id
            """.trimIndent(),
        )
            .param("id", id)
            .param("status", AnnouncementStatus.ARCHIVED.name)
            .param("updatedBy", user.id)
            .param("updatedTime", now)
            .update()
        if (updated == 0) {
            throw notFound()
        }
        return getOrThrow(id, viewerId = user.id)
    }

    @Transactional
    fun schedule(id: UUID, publishAt: LocalDateTime, user: AuthenticatedUser): AnnouncementResponse {
        val current = getOrThrow(id, viewerId = user.id)
        if (current.status != AnnouncementStatus.DRAFT) {
            throw BusinessException(
                HttpStatus.CONFLICT.value(),
                "only draft announcements can be scheduled",
                HttpStatus.CONFLICT,
            )
        }
        val now = LocalDateTime.now(clock)
        jdbcClient.sql(
            """
            update announcements
            set publish_at = :publishAt,
                updated_by = :updatedBy,
                updated_time = :updatedTime
            where id = :id
            """.trimIndent(),
        )
            .param("id", id)
            .param("publishAt", publishAt)
            .param("updatedBy", user.id)
            .param("updatedTime", now)
            .update()
        return getOrThrow(id, viewerId = user.id)
    }

    @Transactional
    fun clearSchedule(id: UUID, user: AuthenticatedUser): AnnouncementResponse {
        val current = getOrThrow(id, viewerId = user.id)
        if (current.status != AnnouncementStatus.DRAFT) {
            throw BusinessException(
                HttpStatus.CONFLICT.value(),
                "only draft announcements can clear schedule",
                HttpStatus.CONFLICT,
            )
        }
        val now = LocalDateTime.now(clock)
        jdbcClient.sql(
            """
            update announcements
            set publish_at = null,
                updated_by = :updatedBy,
                updated_time = :updatedTime
            where id = :id
            """.trimIndent(),
        )
            .param("id", id)
            .param("updatedBy", user.id)
            .param("updatedTime", now)
            .update()
        return getOrThrow(id, viewerId = user.id)
    }

    @Transactional
    fun markRead(id: UUID, user: AuthenticatedUser): AnnouncementResponse {
        val current = getOrThrow(id, viewerId = user.id)
        if (current.status != AnnouncementStatus.PUBLISHED) {
            throw BusinessException(
                HttpStatus.CONFLICT.value(),
                "only published announcements can be marked as read",
                HttpStatus.CONFLICT,
            )
        }
        val now = LocalDateTime.now(clock)
        jdbcClient.sql(
            """
            merge into announcement_reads (announcement_id, user_id, read_at)
            key (announcement_id, user_id)
            values (:announcementId, :userId, :readAt)
            """.trimIndent(),
        )
            .param("announcementId", id)
            .param("userId", user.id)
            .param("readAt", now)
            .update()
        return getOrThrow(id, viewerId = user.id)
    }

    @Transactional
    fun delete(id: UUID) {
        val current = getOrThrow(id, viewerId = null)
        if (current.status == AnnouncementStatus.PUBLISHED) {
            throw BusinessException(
                HttpStatus.CONFLICT.value(),
                "published announcement must be archived before deletion",
                HttpStatus.CONFLICT,
            )
        }
        val deleted = jdbcClient.sql("delete from announcements where id = :id")
            .param("id", id)
            .update()
        if (deleted == 0) {
            throw notFound()
        }
    }

    private fun getOrThrow(id: UUID, viewerId: UUID?): AnnouncementResponse = jdbcClient.sql(
        """
        select a.id, a.title, a.summary, a.content, a.status, a.priority,
               a.published_at, a.publish_at,
               (select count(*) from announcement_reads r where r.announcement_id = a.id) as read_count,
               case when :viewer is null then 0
                    when exists (
                        select 1 from announcement_reads r
                        where r.announcement_id = a.id and r.user_id = :viewer
                    ) then 1 else 0 end as read_by_me,
               a.created_by, a.updated_by, a.created_time, a.updated_time
        from announcements a
        where a.id = :id
        """.trimIndent(),
    )
        .param("id", id)
        .param("viewer", viewerId)
        .query(::mapAnnouncement)
        .optional()
        .orElseThrow { notFound() }

    private fun notFound(): BusinessException =
        BusinessException(HttpStatus.NOT_FOUND.value(), "announcement not found", HttpStatus.NOT_FOUND)

    private fun mapAnnouncement(rs: ResultSet, rowNumber: Int): AnnouncementResponse = AnnouncementResponse(
        id = rs.getObject("id", UUID::class.java),
        title = rs.getString("title"),
        summary = rs.getString("summary"),
        content = rs.getString("content"),
        status = AnnouncementStatus.valueOf(rs.getString("status")),
        priority = rs.getInt("priority"),
        publishedAt = rs.getTimestamp("published_at")?.toLocalDateTime(),
        publishAt = rs.getTimestamp("publish_at")?.toLocalDateTime(),
        readCount = rs.getInt("read_count"),
        readByMe = rs.getInt("read_by_me") > 0,
        createdBy = rs.getObject("created_by", UUID::class.java),
        updatedBy = rs.getObject("updated_by", UUID::class.java),
        createdTime = rs.getTimestamp("created_time").toLocalDateTime(),
        updatedTime = rs.getTimestamp("updated_time").toLocalDateTime(),
    )
}
