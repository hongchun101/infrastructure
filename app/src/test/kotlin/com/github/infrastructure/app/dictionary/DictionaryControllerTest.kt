package com.github.infrastructure.app.dictionary

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.infrastructure.app.EmbeddedRedisTestConfiguration
import com.github.infrastructure.app.InfrastructureApplication
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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.util.UUID

@SpringBootTest(classes = [InfrastructureApplication::class, EmbeddedRedisTestConfiguration::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DictionaryControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {
    @Test
    fun `dictionary endpoints require authentication`() {
        mockMvc.get("/dictionaries").andExpect { status { isUnauthorized() } }
        mockMvc.get("/dictionaries/anything").andExpect { status { isUnauthorized() } }
        mockMvc.post("/dictionaries") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"code":"x","name":"x"}"""
        }.andExpect { status { isUnauthorized() } }
        mockMvc.get("/dictionaries/anything/items").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `category crud lifecycle`() {
        val token = bearer()
        val code = uniqueCode("status")
        val created = createCategory(token, code, "User Status", "Enabled or not")

        assertEquals(code, created.get("data").get("code").asText())
        assertEquals("User Status", created.get("data").get("name").asText())
        assertEquals(true, created.get("data").get("enabled").asBoolean())

        val categoryId = created.get("data").get("id").asText()

        val fetched = mockMvc.get("/dictionaries/$code") {
            header("Authorization", "Bearer $token")
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals(categoryId, fetched.get("data").get("id").asText())

        val updated = mockMvc.put("/dictionaries/$categoryId") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"User Status v2","description":null,"enabled":false}"""
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals("User Status v2", updated.get("data").get("name").asText())
        assertEquals(false, updated.get("data").get("enabled").asBoolean())
        assertTrue(updated.get("data").get("description").isNull)

        val duplicate = mockMvc.post("/dictionaries") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"code":"$code","name":"dup"}"""
        }
            .andExpect { status { isConflict() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals(409, duplicate.get("code").asInt())

        mockMvc.delete("/dictionaries/$categoryId") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }

        mockMvc.get("/dictionaries/$code") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `category delete cascades to items`() {
        val token = bearer()
        val code = uniqueCode("cascade")
        val categoryId = createCategory(token, code, "Cascade").get("data").get("id").asText()
        val itemId = createItem(token, code, "ROOT").get("data").get("id").asText()

        mockMvc.delete("/dictionaries/$categoryId") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }

        val list = mockMvc.get("/dictionaries/$code/items") {
            header("Authorization", "Bearer $token")
        }
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `item supports tree hierarchy and rejects cross category parent`() {
        val token = bearer()
        val codeA = uniqueCode("treeA")
        val codeB = uniqueCode("treeB")
        createCategory(token, codeA, "Tree A")
        createCategory(token, codeB, "Tree B")

        val parent = createItem(token, codeA, "PARENT").get("data")
        val parentId = parent.get("id").asText()

        val child = createItem(token, codeA, "CHILD", parentId = parentId).get("data")
        assertEquals(parentId, child.get("parentId").asText())

        val topLevel = mockMvc.get("/dictionaries/$codeA/items") {
            header("Authorization", "Bearer $token")
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals(1, topLevel.get("data").size())
        assertEquals("PARENT", topLevel.get("data").first().get("code").asText())

        val children = mockMvc.get("/dictionaries/$codeA/items") {
            header("Authorization", "Bearer $token")
            param("parentId", parentId)
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals(1, children.get("data").size())
        assertEquals("CHILD", children.get("data").first().get("code").asText())

        val crossCategory = mockMvc.post("/dictionaries/$codeB/items") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"code":"X","name":"X","parentId":"$parentId"}"""
        }
            .andExpect { status { isBadRequest() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals(400, crossCategory.get("code").asInt())

        val missingParent = mockMvc.post("/dictionaries/$codeA/items") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"code":"Y","name":"Y","parentId":"${UUID.randomUUID()}"}"""
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `item cannot be its own parent and cannot delete item with children`() {
        val token = bearer()
        val code = uniqueCode("treeC")
        createCategory(token, code, "Tree C")
        val parentId = createItem(token, code, "P").get("data").get("id").asText()
        val childId = createItem(token, code, "C", parentId = parentId).get("data").get("id").asText()

        val selfParent = mockMvc.put("/dictionaries/items/$childId") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"C","parentId":"$childId","sortOrder":0,"enabled":true}"""
        }
            .andExpect { status { isBadRequest() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals(400, selfParent.get("code").asInt())

        val deleteParent = mockMvc.delete("/dictionaries/items/$parentId") {
            header("Authorization", "Bearer $token")
        }
            .andExpect { status { isConflict() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals(409, deleteParent.get("code").asInt())
        assertTrue(deleteParent.get("message").asText().contains("child"))

        mockMvc.delete("/dictionaries/items/$childId") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }

        mockMvc.delete("/dictionaries/items/$parentId") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `duplicate item code in same category returns conflict`() {
        val token = bearer()
        val code = uniqueCode("dupItem")
        createCategory(token, code, "Dup")
        createItem(token, code, "FIRST")

        val dup = mockMvc.post("/dictionaries/$code/items") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"code":"FIRST","name":"again"}"""
        }
            .andExpect { status { isConflict() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals(409, dup.get("code").asInt())
    }

    @Test
    fun `validation errors use unified envelope`() {
        val token = bearer()

        val blankCode = mockMvc.post("/dictionaries") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"code":"","name":"x"}"""
        }
            .andExpect { status { isBadRequest() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals(400, blankCode.get("code").asInt())
        assertNotNull(blankCode.get("message").asText())
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

    private fun createCategory(
        token: String,
        code: String,
        name: String,
        description: String? = null,
        enabled: Boolean = true,
    ): JsonNode = mockMvc.post("/dictionaries") {
        header("Authorization", "Bearer $token")
        contentType = MediaType.APPLICATION_JSON
        content = """{"code":"$code","name":"$name","description":${jsonString(description)},"enabled":$enabled}"""
    }
        .andExpect { status { isOk() } }
        .andReturn()
        .response
        .contentAsString
        .let(objectMapper::readTree)

    private fun createItem(
        token: String,
        categoryCode: String,
        code: String,
        name: String = code,
        parentId: String? = null,
        sortOrder: Int = 0,
        enabled: Boolean = true,
    ): JsonNode = mockMvc.post("/dictionaries/$categoryCode/items") {
        header("Authorization", "Bearer $token")
        contentType = MediaType.APPLICATION_JSON
        val parentPart = if (parentId == null) "" else ""","parentId":"$parentId""""
        content = """{"code":"$code","name":"$name","sortOrder":$sortOrder,"enabled":$enabled$parentPart}"""
    }
        .andExpect { status { isOk() } }
        .andReturn()
        .response
        .contentAsString
        .let(objectMapper::readTree)

    private fun jsonString(value: String?): String = if (value == null) "null" else "\"$value\""
}