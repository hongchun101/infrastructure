package com.github.infrastructure.app.project

import com.github.infrastructure.core.web.exception.BusinessException
import com.github.infrastructure.security.context.AuthenticatedUser
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val clock: Clock,
) {
    @Transactional
    fun create(request: CreateProjectRequest, user: AuthenticatedUser): ProjectResponse {
        val createdTime = LocalDateTime.now(clock)
        val project = projectRepository.save(
            Project {
                id = UUID.randomUUID()
                name = request.name
                ownerId = user.id
                this.createdTime = createdTime
            },
        )
        return project.toResponse()
    }

    fun list(user: AuthenticatedUser): List<ProjectResponse> =
        projectRepository.findByOwnerId(user.id).map { it.toResponse() }

    fun get(id: UUID, user: AuthenticatedUser): ProjectResponse =
        projectRepository.findByIdAndOwnerId(id, user.id)?.toResponse()
            ?: throw BusinessException(HttpStatus.NOT_FOUND.value(), "resource not found", HttpStatus.NOT_FOUND)

    private fun Project.toResponse(): ProjectResponse = ProjectResponse(
        id = id,
        name = name,
        ownerId = ownerId,
        createdTime = createdTime,
    )
}