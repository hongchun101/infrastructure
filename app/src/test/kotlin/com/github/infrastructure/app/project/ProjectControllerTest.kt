package com.github.infrastructure.app.project

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.infrastructure.app.InfrastructureApplication
import com.github.infrastructure.app.EmbeddedRedisTestConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
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
class ProjectControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {
    @Test
    fun `project endpoints are protected and do not use api prefix`() {
        mockMvc.get("/projects")
            .andExpect { status { isUnauthorized() } }
        val token = login().get("data").get("accessToken").asText()
        mockMvc.get("/api/projects") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `authenticated user creates and reads project`() {
        val token = login().get("data").get("accessToken").asText()

        val created = mockMvc.post("/projects") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"base scaffold"}"""
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals(0, created.get("code").asInt())
        assertEquals("base scaffold", created.get("data").get("name").asText())
        val projectId = created.get("data").get("id").asText()
        assertTrue(projectId.isNotBlank())

        val fetched = mockMvc.get("/projects/$projectId") {
            header("Authorization", "Bearer $token")
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals(projectId, fetched.get("data").get("id").asText())
        assertEquals("base scaffold", fetched.get("data").get("name").asText())
    }

    @Test
    fun `validation errors use unified response`() {
        val token = login().get("data").get("accessToken").asText()

        val response = mockMvc.post("/projects") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":""}"""
        }
            .andExpect { status { isBadRequest() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals(400, response.get("code").asInt())
        assertNotNull(response.get("message").asText())
    }

    private fun login(): JsonNode = mockMvc.post("/auth/login") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"username":"admin","password":"admin123"}"""
    }
        .andExpect { status { isOk() } }
        .andReturn()
        .response
        .contentAsString
        .let(objectMapper::readTree)
}
