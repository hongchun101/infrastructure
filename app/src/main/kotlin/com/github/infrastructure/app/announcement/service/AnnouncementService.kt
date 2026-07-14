package com.github.infrastructure.app.announcement.service

import com.github.infrastructure.app.announcement.dto.AnnouncementResponse
import com.github.infrastructure.app.announcement.dto.CreateAnnouncementRequest
import com.github.infrastructure.app.announcement.dto.ScheduleAnnouncementRequest
import com.github.infrastructure.app.announcement.dto.UpdateAnnouncementRequest
import com.github.infrastructure.app.announcement.entity.Announcement
import com.github.infrastructure.app.announcement.entity.AnnouncementStatus
import com.github.infrastructure.app.announcement.repository.AnnouncementReadRepository
import com.github.infrastructure.app.announcement.repository.AnnouncementRepository
import com.github.infrastructure.core.web.exception.BusinessException
import com.github.infrastructure.security.context.AuthenticatedUser
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class AnnouncementService(
    private val announcementRepository: AnnouncementRepository,
    private val announcementReadRepository: AnnouncementReadRepository,
    private val clock: Clock,
) {
    @Transactional
    fun create(request: CreateAnnouncementRequest, user: AuthenticatedUser): AnnouncementResponse {
        val now = LocalDateTime.now(clock)
        val announcement = announcementRepository.save(
            Announcement {
                id = UUID.randomUUID()
                title = request.title
                summary = request.summary
                content = request.content
                status = AnnouncementStatus.DRAFT.name
                priority = request.priority
                publishedAt = null
                publishAt = null
                createdBy = user.id
                updatedBy = user.id
                createdTime = now
                updatedTime = now
            },
        ).modifiedEntity
        return announcement.toResponse(readCount = 0L, readByMe = false)
    }

    fun list(
        status: AnnouncementStatus?,
        keyword: String?,
        unreadOnly: Boolean,
        user: AuthenticatedUser,
    ): List<AnnouncementResponse> {
        val announcements = announcementRepository.list(
            status = status?.name,
            keyword = keyword,
            viewerId = user.id,
            unreadOnly = unreadOnly,
        )
        if (announcements.isEmpty()) return emptyList()
        val ids = announcements.map { it.id }
        val readCounts = announcementRepository.countReadsByAnnouncementIds(ids)
        val readIds = if (unreadOnly) {
            emptySet()
        } else {
            announcementRepository.findReadAnnouncementIds(user.id, ids).toSet()
        }
        return announcements.map {
            it.toResponse(
                readCount = readCounts[it.id] ?: 0,
                readByMe = readIds.contains(it.id),
            )
        }
    }

    fun get(id: UUID, user: AuthenticatedUser): AnnouncementResponse =
        findOneOrThrow(id).let { (announcement, readCount) ->
            announcement.toResponse(
                readCount = readCount,
                readByMe = announcementRepository.existsReadByUser(id, user.id),
            )
        }

    @Transactional
    fun update(id: UUID, request: UpdateAnnouncementRequest, user: AuthenticatedUser): AnnouncementResponse {
        val current = findOneOrThrow(id).first
        if (current.status == AnnouncementStatus.ARCHIVED.name) {
            throw BusinessException(
                HttpStatus.CONFLICT.value(),
                "archived announcement cannot be edited",
                HttpStatus.CONFLICT,
            )
        }
        val now = LocalDateTime.now(clock)
        val saved = announcementRepository.save(
            Announcement {
                this.id = current.id
                title = request.title
                summary = request.summary
                content = request.content
                status = current.status
                priority = request.priority
                publishedAt = current.publishedAt
                publishAt = current.publishAt
                createdBy = current.createdBy
                updatedBy = user.id
                createdTime = current.createdTime
                updatedTime = now
            },
        ).modifiedEntity
        return getOrThrow(saved.id, user.id)
    }

    @Transactional
    fun publish(id: UUID, user: AuthenticatedUser): AnnouncementResponse {
        val current = findOneOrThrow(id).first
        if (current.status == AnnouncementStatus.PUBLISHED.name) return getOrThrow(id, user.id)
        if (current.status == AnnouncementStatus.ARCHIVED.name) {
            throw BusinessException(
                HttpStatus.CONFLICT.value(),
                "archived announcement cannot be republished",
                HttpStatus.CONFLICT,
            )
        }
        val now = LocalDateTime.now(clock)
        val scheduledPublishAt = current.publishAt
        if (scheduledPublishAt != null && scheduledPublishAt.isAfter(now)) {
            throw BusinessException(
                HttpStatus.CONFLICT.value(),
                "scheduled publish time has not been reached",
                HttpStatus.CONFLICT,
            )
        }
        val newPublishedAt = scheduledPublishAt ?: now
        announcementRepository.save(
            Announcement {
                this.id = current.id
                title = current.title
                summary = current.summary
                content = current.content
                priority = current.priority
                publishedAt = newPublishedAt
                publishAt = current.publishAt
                createdBy = current.createdBy
                updatedBy = user.id
                createdTime = current.createdTime
                status = AnnouncementStatus.PUBLISHED.name
                updatedTime = now
            },
        )
        return getOrThrow(id, user.id)
    }

    @Transactional
    fun archive(id: UUID, user: AuthenticatedUser): AnnouncementResponse {
        val current = findOneOrThrow(id).first
        if (current.status == AnnouncementStatus.ARCHIVED.name) return getOrThrow(id, user.id)
        val now = LocalDateTime.now(clock)
        announcementRepository.save(
            Announcement {
                this.id = current.id
                title = current.title
                summary = current.summary
                content = current.content
                priority = current.priority
                publishedAt = current.publishedAt
                publishAt = current.publishAt
                createdBy = current.createdBy
                updatedBy = user.id
                createdTime = current.createdTime
                status = AnnouncementStatus.ARCHIVED.name
                updatedTime = now
            },
        )
        return getOrThrow(id, user.id)
    }

    @Transactional
    fun schedule(id: UUID, publishAt: LocalDateTime, user: AuthenticatedUser): AnnouncementResponse {
        val current = findOneOrThrow(id).first
        if (current.status != AnnouncementStatus.DRAFT.name) {
            throw BusinessException(
                HttpStatus.CONFLICT.value(),
                "only draft announcements can be scheduled",
                HttpStatus.CONFLICT,
            )
        }
        val now = LocalDateTime.now(clock)
        announcementRepository.save(
            Announcement {
                this.id = current.id
                title = current.title
                summary = current.summary
                content = current.content
                priority = current.priority
                publishedAt = current.publishedAt
                this.publishAt = publishAt
                createdBy = current.createdBy
                updatedBy = user.id
                createdTime = current.createdTime
                status = current.status
                updatedTime = now
            },
        )
        return getOrThrow(id, user.id)
    }

    @Transactional
    fun clearSchedule(id: UUID, user: AuthenticatedUser): AnnouncementResponse {
        val current = findOneOrThrow(id).first
        if (current.status != AnnouncementStatus.DRAFT.name) {
            throw BusinessException(
                HttpStatus.CONFLICT.value(),
                "only draft announcements can clear schedule",
                HttpStatus.CONFLICT,
            )
        }
        val now = LocalDateTime.now(clock)
        announcementRepository.save(
            Announcement {
                this.id = current.id
                title = current.title
                summary = current.summary
                content = current.content
                priority = current.priority
                publishedAt = current.publishedAt
                publishAt = null
                createdBy = current.createdBy
                updatedBy = user.id
                createdTime = current.createdTime
                status = current.status
                updatedTime = now
            },
        )
        return getOrThrow(id, user.id)
    }

    @Transactional
    fun markRead(id: UUID, user: AuthenticatedUser): AnnouncementResponse {
        val current = findOneOrThrow(id).first
        if (current.status != AnnouncementStatus.PUBLISHED.name) {
            throw BusinessException(
                HttpStatus.CONFLICT.value(),
                "only published announcements can be marked as read",
                HttpStatus.CONFLICT,
            )
        }
        val now = LocalDateTime.now(clock)
        announcementReadRepository.markRead(current.id, user.id, now)
        return getOrThrow(id, user.id)
    }

    @Transactional
    fun delete(id: UUID) {
        val current = findOneOrThrow(id).first
        if (current.status == AnnouncementStatus.PUBLISHED.name) {
            throw BusinessException(
                HttpStatus.CONFLICT.value(),
                "published announcement must be archived before deletion",
                HttpStatus.CONFLICT,
            )
        }
        announcementRepository.deleteById(id)
    }

    private fun findOneOrThrow(id: UUID): Pair<Announcement, Long> {
        val announcement = announcementRepository.findById(id) ?: throw notFound()
        val readCount = announcementRepository.countReadsByAnnouncementIds(listOf(id))[id] ?: 0L
        return announcement to readCount
    }

    private fun getOrThrow(id: UUID, userId: UUID?): AnnouncementResponse {
        val (announcement, readCount) = findOneOrThrow(id)
        val readByMe = userId?.let { announcementRepository.existsReadByUser(id, it) } ?: false
        return announcement.toResponse(readCount = readCount, readByMe = readByMe)
    }

    private fun notFound(): BusinessException =
        BusinessException(HttpStatus.NOT_FOUND.value(), "announcement not found", HttpStatus.NOT_FOUND)

    private fun Announcement.toResponse(readCount: Long, readByMe: Boolean): AnnouncementResponse =
        AnnouncementResponse(
            id = id,
            title = title,
            summary = summary,
            content = content,
            status = AnnouncementStatus.valueOf(status),
            priority = priority,
            publishedAt = publishedAt,
            publishAt = publishAt,
            readCount = readCount,
            readByMe = readByMe,
            createdBy = createdBy,
            updatedBy = updatedBy,
            createdTime = createdTime,
            updatedTime = updatedTime,
        )
}
