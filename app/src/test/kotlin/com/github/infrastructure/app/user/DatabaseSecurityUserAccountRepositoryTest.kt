package com.github.infrastructure.app.user

import com.github.infrastructure.app.InfrastructureApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

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
        assertEquals(listOf("project:read", "project:write"), account.permissions)
    }
}
