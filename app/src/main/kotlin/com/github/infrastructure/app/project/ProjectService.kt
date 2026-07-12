package com.github.infrastructure.app.project

import com.github.infrastructure.core.web.exception.BusinessException
import com.github.infrastructure.security.context.AuthenticatedUser
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class ProjectService(
    private val jdbcClient: JdbcClient,
    private val clock: Clock,
) {
    @Transactional
    fun create(request: CreateProjectRequest, user: AuthenticatedUser): ProjectResponse {
        val id = UUID.randomUUID()
        val createdTime = LocalDateTime.now(clock)
        jdbcClient.sql(
            """
            insert into projects (id, name, owner_id, created_time)
            values (:id, :name, :ownerId, :createdTime)
            """.trimIndent(),
        )
            .param("id", id)
            .param("name", request.name)
            .param("ownerId", user.id)
            .param("createdTime", createdTime)
            .update()
        return ProjectResponse(id = id, name = request.name, ownerId = user.id, createdTime = createdTime)
    }

    fun list(user: AuthenticatedUser): List<ProjectResponse> = jdbcClient.sql(
        """
        select id, name, owner_id, created_time
        from projects
        where owner_id = :ownerId
        order by created_time desc
        """.trimIndent(),
    )
        .param("ownerId", user.id)
        .query(::mapProject)
        .list()

    fun get(id: UUID, user: AuthenticatedUser): ProjectResponse = jdbcClient.sql(
        """
        select id, name, owner_id, created_time
        from projects
        where id = :id and owner_id = :ownerId
        """.trimIndent(),
    )
        .param("id", id)
        .param("ownerId", user.id)
        .query(::mapProject)
        .optional()
        .orElseThrow { BusinessException(HttpStatus.NOT_FOUND.value(), "resource not found", HttpStatus.NOT_FOUND) }

    private fun mapProject(rs: java.sql.ResultSet, rowNumber: Int): ProjectResponse = ProjectResponse(
        id = rs.getObject("id", UUID::class.java),
        name = rs.getString("name"),
        ownerId = rs.getObject("owner_id", UUID::class.java),
        createdTime = rs.getTimestamp("created_time").toLocalDateTime(),
    )
}
