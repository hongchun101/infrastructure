package com.github.infrastructure.app.auth

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.infrastructure.app.InfrastructureApplication
import com.github.infrastructure.app.EmbeddedRedisTestConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest(classes = [InfrastructureApplication::class, EmbeddedRedisTestConfiguration::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {
    @Test
    fun `login returns uuid tokens stored server side and me uses access token`() {
        val login = login("admin", "admin123")
        val data = login.get("data")
        val accessToken = data.get("accessToken").asText()
        val refreshToken = data.get("refreshToken").asText()

        assertUuid(accessToken)
        assertUuid(refreshToken)
        assertEquals(1800, data.get("accessTokenExpiresInSeconds").asLong())
        assertEquals(604800, data.get("refreshTokenExpiresInSeconds").asLong())

        val me = mockMvc.get("/me") {
            header("Authorization", "Bearer $accessToken")
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals("admin", me.get("data").get("username").asText())
        assertEquals("ADMIN", me.get("data").get("roles").first().asText())
        val permissions = me.get("data").get("permissions").map { it.asText() }
        assertTrue(permissions.contains("project:read"))
        assertTrue(permissions.contains("project:write"))
        assertTrue(permissions.contains("announcement:read"))
        assertTrue(permissions.contains("announcement:write"))
    }

    @Test
    fun `login supports email and phone modes`() {
        val emailLogin = login(LoginModePayload("EMAIL", "admin@example.com", "admin123")).get("data")
        val phoneLogin = login(LoginModePayload("PHONE", "13800000000", "admin123")).get("data")

        assertUuid(emailLogin.get("accessToken").asText())
        assertUuid(phoneLogin.get("accessToken").asText())

        mockMvc.get("/me") {
            header("Authorization", "Bearer ${emailLogin.get("accessToken").asText()}")
        }.andExpect { status { isOk() } }

        mockMvc.get("/me") {
            header("Authorization", "Bearer ${phoneLogin.get("accessToken").asText()}")
        }.andExpect { status { isOk() } }
    }
    @Test
    fun `backend account login is isolated from c end user account`() {
        val response = login("""{"accountType":"BACKEND","username":"operator","password":"admin123"}""")
        assertEquals(0, response.get("code").asInt())
    }

    @Test
    fun `bad credentials disabled users and invalid token return unauthorized envelope`() {
        val badPassword = loginExpectingUnauthorized("admin", "wrong")
        assertEquals(401, badPassword.get("code").asInt())
        assertEquals("unauthorized", badPassword.get("message").asText())

        val disabled = loginExpectingUnauthorized("disabled", "admin123")
        assertEquals(401, disabled.get("code").asInt())

        val invalidToken = mockMvc.get("/me") {
            header("Authorization", "Bearer not-a-real-token")
        }
            .andExpect { status { isUnauthorized() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals(401, invalidToken.get("code").asInt())
        assertEquals("unauthorized", invalidToken.get("message").asText())
    }

    @Test
    fun `refresh rotates token pair and old tokens stop working`() {
        val first = login("admin", "admin123").get("data")
        val refreshed = mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":"${first.get("refreshToken").asText()}"}"""
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)
            .get("data")

        assertNotEquals(first.get("accessToken").asText(), refreshed.get("accessToken").asText())
        assertNotEquals(first.get("refreshToken").asText(), refreshed.get("refreshToken").asText())

        mockMvc.get("/me") {
            header("Authorization", "Bearer ${first.get("accessToken").asText()}")
        }.andExpect { status { isUnauthorized() } }

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":"${first.get("refreshToken").asText()}"}"""
        }.andExpect { status { isUnauthorized() } }

        mockMvc.get("/me") {
            header("Authorization", "Bearer ${refreshed.get("accessToken").asText()}")
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `logout invalidates current access and refresh tokens`() {
        val tokens = login("admin", "admin123").get("data")
        val accessToken = tokens.get("accessToken").asText()
        val refreshToken = tokens.get("refreshToken").asText()

        mockMvc.post("/auth/logout") {
            header("Authorization", "Bearer $accessToken")
        }.andExpect { status { isOk() } }

        mockMvc.get("/me") {
            header("Authorization", "Bearer $accessToken")
        }.andExpect { status { isUnauthorized() } }

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":"$refreshToken"}"""
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `trace id is present for success and error responses`() {
        val loginResponse = mockMvc.post("/auth/login") {
            header("X-Trace-Id", "trace-from-client")
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"admin","password":"admin123"}"""
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response

        assertEquals("trace-from-client", loginResponse.getHeader("X-Trace-Id"))

        val errorResponse = mockMvc.get("/me")
            .andExpect { status { isUnauthorized() } }
            .andReturn()
            .response

        assertNotNull(errorResponse.getHeader("X-Trace-Id"))
    }

    private fun login(username: String, password: String): JsonNode = login("""{"username":"$username","password":"$password"}""")

    private fun login(payload: LoginModePayload): JsonNode = login("""{"mode":"${payload.mode}","principal":"${payload.principal}","password":"${payload.password}"}""")

    private fun login(json: String): JsonNode = mockMvc.post("/auth/login") {
        contentType = MediaType.APPLICATION_JSON
        content = json
    }
        .andExpect { status { isOk() } }
        .andReturn()
        .response
        .contentAsString
        .let(objectMapper::readTree)

    private fun loginExpectingUnauthorized(username: String, password: String): JsonNode = mockMvc.post("/auth/login") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"username":"$username","password":"$password"}"""
    }
        .andExpect { status { isUnauthorized() } }
        .andReturn()
        .response
        .contentAsString
        .let(objectMapper::readTree)


    private data class LoginModePayload(
        val mode: String,
        val principal: String,
        val password: String,
    )
    private fun assertUuid(value: String) {
        val uuidPattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        assertTrue(uuidPattern.matches(value), "$value is not a UUID")
    }
}
