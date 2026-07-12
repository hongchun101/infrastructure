package com.github.infrastructure.app.user

import com.github.infrastructure.app.InfrastructureApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import com.github.infrastructure.security.auth.LoginMode

@SpringBootTest(classes = [InfrastructureApplication::class])
@ActiveProfiles("test")
class DatabaseSecurityUserAccountRepositoryTest(
    @Autowired private val repository: DatabaseSecurityUserAccountRepository,
) {
    @Test
    fun `loads seeded admin account with roles and permissions`() {
        val account = repository.findByUsername("admin")

        assertNotNull(account)
        assertEquals("admin", account!!.username)
        assertEquals("Administrator", account.displayName)
        assertEquals(true, account.enabled)
        assertEquals(listOf("ADMIN"), account.roles)
        assertEquals(
            setOf("project:read", "project:write", "operation:log:read"),
            account.permissions.toSet(),
        )
    }

    @Test
    fun `loads seeded admin account by email and phone login modes`() {
        val emailAccount = repository.findForLogin(LoginMode.EMAIL, "admin@example.com")
        val phoneAccount = repository.findForLogin(LoginMode.PHONE, "13800000000")

        assertNotNull(emailAccount)
        assertNotNull(phoneAccount)
        assertEquals("admin", emailAccount!!.username)
        assertEquals("admin", phoneAccount!!.username)
    }
}
