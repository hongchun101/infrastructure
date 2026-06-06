package com.github.infrastructure.app.user

import com.github.infrastructure.security.SecurityUserAccount
import com.github.infrastructure.security.SecurityUserAccountRepository
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class DatabaseSecurityUserAccountRepository(
    private val jdbcClient: JdbcClient,
) : SecurityUserAccountRepository {
    override fun findByUsername(username: String): SecurityUserAccount? {
        val user = jdbcClient.sql(
            """
            select id, username, password_hash, display_name, enabled
            from users
            where username = :username
            """.trimIndent(),
        )
            .param("username", username)
            .query { rs, _ ->
                UserRow(
                    id = rs.getObject("id", UUID::class.java),
                    username = rs.getString("username"),
                    passwordHash = rs.getString("password_hash"),
                    displayName = rs.getString("display_name"),
                    enabled = rs.getBoolean("enabled"),
                )
            }
            .optional()
            .orElse(null) ?: return null

        return SecurityUserAccount(
            id = user.id,
            username = user.username,
            passwordHash = user.passwordHash,
            displayName = user.displayName,
            enabled = user.enabled,
            roles = roles(user.id),
            permissions = permissions(user.id),
        )
    }

    private fun roles(userId: UUID): List<String> = jdbcClient.sql(
        """
        select r.code
        from roles r
        join user_roles ur on ur.role_id = r.id
        where ur.user_id = :userId
        order by r.code
        """.trimIndent(),
    )
        .param("userId", userId)
        .query(String::class.java)
        .list()

    private fun permissions(userId: UUID): List<String> = jdbcClient.sql(
        """
        select distinct p.code
        from permissions p
        join role_permissions rp on rp.permission_id = p.id
        join user_roles ur on ur.role_id = rp.role_id
        where ur.user_id = :userId
        order by p.code
        """.trimIndent(),
    )
        .param("userId", userId)
        .query(String::class.java)
        .list()

    private data class UserRow(
        val id: UUID,
        val username: String,
        val passwordHash: String,
        val displayName: String,
        val enabled: Boolean,
    )
}
