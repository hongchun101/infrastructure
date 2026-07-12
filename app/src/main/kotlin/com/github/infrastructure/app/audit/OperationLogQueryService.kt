package com.github.infrastructure.app.audit

import com.github.infrastructure.core.web.exception.BusinessException
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service

@Service
class OperationLogQueryService(
    private val jdbcClient: JdbcClient,
) {
    fun list(
        module: String?,
        action: String?,
        userId: UUID?,
        success: Boolean?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        page: Int,
        size: Int,
    ): PageResponse<OperationLogResponse> {
        if (page < 0) {
            throw BusinessException(HttpStatus.BAD_REQUEST.value(), "page must be >= 0", HttpStatus.BAD_REQUEST)
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw BusinessException(
                HttpStatus.BAD_REQUEST.value(),
                "size must be in (0, $MAX_PAGE_SIZE]",
                HttpStatus.BAD_REQUEST,
            )
        }
        val conditions = mutableListOf<String>()
        val params = mutableMapOf<String, Any?>()
        module?.takeIf { it.isNotBlank() }?.let {
            conditions += "module = :module"
            params["module"] = it
        }
        action?.takeIf { it.isNotBlank() }?.let {
            conditions += "action = :action"
            params["action"] = it
        }
        userId?.let {
            conditions += "user_id = :userId"
            params["userId"] = it
        }
        success?.let {
            conditions += "success = :success"
            params["success"] = it
        }
        startTime?.let {
            conditions += "created_time >= :startTime"
            params["startTime"] = it
        }
        endTime?.let {
            conditions += "created_time < :endTime"
            params["endTime"] = it
        }
        val whereClause = if (conditions.isEmpty()) "" else " where " + conditions.joinToString(" and ")

        val total = run {
            var q = jdbcClient.sql("select count(*) from operation_logs$whereClause")
            params.forEach { (k, v) -> q = q.param(k, v) }
            q.query(Long::class.java).single()
        }
        if (total == 0L) {
            return PageResponse(emptyList(), 0L, page, size)
        }
        val offset = page.toLong() * size.toLong()
        val items = run {
            var q = jdbcClient.sql(
                """
                select id, trace_id, user_id, username, module, action, description,
                       method, path, query_string, response_status, error_message,
                       client_ip, user_agent, duration_ms, success, created_time
                from operation_logs$whereClause
                order by created_time desc, id desc
                limit :limit offset :offset
                """.trimIndent(),
            )
            params.forEach { (k, v) -> q = q.param(k, v) }
            q.param("limit", size.toLong())
                .param("offset", offset)
                .query(::mapLog)
                .list()
        }
        return PageResponse(items, total, page, size)
    }

    private fun mapLog(rs: ResultSet, rowNumber: Int): OperationLogResponse = OperationLogResponse(
        id = rs.getObject("id", UUID::class.java),
        traceId = rs.getString("trace_id"),
        userId = rs.getObject("user_id", UUID::class.java),
        username = rs.getString("username"),
        module = rs.getString("module"),
        action = rs.getString("action"),
        description = rs.getString("description"),
        method = rs.getString("method"),
        path = rs.getString("path"),
        queryString = rs.getString("query_string"),
        responseStatus = rs.getInt("response_status"),
        errorMessage = rs.getString("error_message"),
        clientIp = rs.getString("client_ip"),
        userAgent = rs.getString("user_agent"),
        durationMs = rs.getLong("duration_ms"),
        success = rs.getBoolean("success"),
        createdTime = rs.getTimestamp("created_time").toLocalDateTime(),
    )

    companion object {
        private const val MAX_PAGE_SIZE = 200
    }
}