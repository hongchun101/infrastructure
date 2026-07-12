package com.github.infrastructure.app.audit

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.infrastructure.app.EmbeddedRedisTestConfiguration
import com.github.infrastructure.app.InfrastructureApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID

@SpringBootTest(classes = [InfrastructureApplication::class, EmbeddedRedisTestConfiguration::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OperationLogAspectTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {
    @Test
    fun `annotated endpoint writes operation log entry`() {
        val token = bearer()
        val code = uniqueCode("audit-ok")

        createCategory(token, code, "Audit OK")

        val log = awaitLog(token, module = "dictionary", action = "create-category") {
            it.get("path").asText() == "/dictionaries" &&
                it.get("method").asText() == "POST" &&
                it.get("success").asBoolean() &&
                it.get("description").asText() == "Create dictionary category"
        }

        assertEquals("admin", log.get("username").asText())
        assertNotNull(log.get("userId").asText())
        assertEquals(200, log.get("responseStatus").asInt())
        assertTrue(log.get("durationMs").asLong() >= 0)
        assertNotNull(log.get("createdTime").asText())
        assertNotNull(log.get("id").asText())
    }

    @Test
    fun `failed annotated endpoint writes operation log with error message`() {
        val token = bearer()
        val code = uniqueCode("audit-fail")
        createCategory(token, code, "first")

        mockMvc.post("/dictionaries") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"code":"$code","name":"dup"}"""
        }.andExpect { status { isConflict() } }

        val log = awaitLog(token, module = "dictionary", action = "create-category") {
            it.get("path").asText() == "/dictionaries" &&
                it.get("method").asText() == "POST" &&
                !it.get("success").asBoolean() &&
                it.get("responseStatus").asInt() == 409 &&
                it.get("errorMessage").asText().contains("already exists")
        }

        assertEquals(409, log.get("responseStatus").asInt())
        assertFalse(log.get("success").asBoolean())
    }

    @Test
    fun `non-annotated endpoint is not logged`() {
        val token = bearer()
        val totalBefore = totalLogs(token)

        mockMvc.get("/dictionaries") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }

        Thread.sleep(300)

        val totalAfter = totalLogs(token)
        assertEquals(totalBefore, totalAfter, "non-annotated GET /dictionaries must not produce a log")
    }

    @Test
    fun `delete annotated endpoint writes operation log`() {
        val token = bearer()
        val code = uniqueCode("audit-del")
        val categoryId = createCategory(token, code, "Audit Del").get("data").get("id").asText()

        mockMvc.delete("/dictionaries/$categoryId") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }

        val log = awaitLog(token, module = "dictionary", action = "delete-category") {
            it.get("path").asText() == "/dictionaries/{id}" &&
                it.get("method").asText() == "DELETE" &&
                it.get("success").asBoolean()
        }

        assertEquals("/dictionaries/{id}", log.get("path").asText())
        assertEquals("Delete dictionary category", log.get("description").asText())
    }

    @Test
    fun `query supports module filter success filter and pagination`() {
        val token = bearer()
        val code = uniqueCode("audit-page")
        createCategory(token, code, "paged")
        awaitLog(token, module = "dictionary", action = "create-category") {
            it.get("description").asText() == "Create dictionary category"
        }

        val response = queryLogs(token, "module=dictionary&action=create-category&success=true&page=0&size=5")
        val data = response.get("data")
        assertTrue(data.get("total").asLong() >= 1)
        assertTrue(data.get("items").size() <= 5)
        assertTrue(data.get("page").asInt() == 0)
        assertTrue(data.get("size").asInt() == 5)

        val failedResponse = queryLogs(token, "module=dictionary&action=create-category&success=false&page=0&size=5")
        assertTrue(failedResponse.get("data").get("total").asLong() >= 1)
    }

    @Test
    fun `query rejects invalid pagination`() {
        val token = bearer()
        mockMvc.get("/operation-logs?size=0") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isBadRequest() } }

        mockMvc.get("/operation-logs?page=-1") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `query endpoint requires authentication`() {
        mockMvc.get("/operation-logs").andExpect { status { isUnauthorized() } }
    }

    private fun bearer(): String = mockMvc.post("/auth/login") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"username":"admin","password":"admin123"}"""
    }
        .andExpect { status { isOk() } }
        .andReturn()
        .response
        .contentAsString
        .let(objectMapper::readTree)
        .get("data")
        .get("accessToken")
        .asText()

    private fun uniqueCode(prefix: String): String =
        "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(10)}"

    private fun createCategory(token: String, code: String, name: String): JsonNode = mockMvc.post("/dictionaries") {
        header("Authorization", "Bearer $token")
        contentType = MediaType.APPLICATION_JSON
        content = """{"code":"$code","name":"$name"}"""
    }
        .andExpect { status { isOk() } }
        .andReturn()
        .response
        .contentAsString
        .let(objectMapper::readTree)

    private fun queryLogs(token: String, query: String): JsonNode = mockMvc.get("/operation-logs?$query") {
        header("Authorization", "Bearer $token")
    }
        .andExpect { status { isOk() } }
        .andReturn()
        .response
        .contentAsString
        .let(objectMapper::readTree)

    private fun totalLogs(token: String): Long = queryLogs(token, "page=0&size=1")
        .get("data")
        .get("total")
        .asLong()

    private fun awaitLog(
        token: String,
        module: String,
        action: String,
        timeoutMs: Long = 5000,
        pollMs: Long = 50,
        matcher: (JsonNode) -> Boolean,
    ): JsonNode {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val response = queryLogs(token, "module=$module&action=$action&page=0&size=50")
            val items = response.get("data").get("items")
            for (i in 0 until items.size()) {
                if (matcher(items.get(i))) return items.get(i)
            }
            Thread.sleep(pollMs)
        }
        throw AssertionError("operation log matching predicate not found within ${timeoutMs}ms")
    }
}