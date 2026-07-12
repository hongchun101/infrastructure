package com.github.infrastructure.app.announcement

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
import org.springframework.test.web.servlet.put
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest(classes = [InfrastructureApplication::class, EmbeddedRedisTestConfiguration::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnnouncementControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {
    @Test
    fun `announcement endpoints require authentication`() {
        mockMvc.get("/announcements").andExpect { status { isUnauthorized() } }
        mockMvc.get("/announcements/${UUID.randomUUID()}").andExpect { status { isUnauthorized() } }
        mockMvc.post("/announcements") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"t","content":"c"}"""
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `admin can create, read, update and delete a draft announcement`() {
        val token = bearer()

        val created = mockMvc.post("/announcements") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"Maintenance window","summary":"Brief downtime","content":"Long body","priority":3}"""
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals(0, created.get("code").asInt())
        val data = created.get("data")
        assertEquals("Maintenance window", data.get("title").asText())
        assertEquals("DRAFT", data.get("status").asText())
        assertEquals(3, data.get("priority").asInt())
        assertTrue(data.get("publishedAt").isNull)
        assertTrue(data.get("publishAt").isNull)
        assertEquals(0, data.get("readCount").asInt())
        assertFalse(data.get("readByMe").asBoolean())
        val announcementId = data.get("id").asText()
        assertTrue(announcementId.isNotBlank())

        val fetched = mockMvc.get("/announcements/$announcementId") {
            header("Authorization", "Bearer $token")
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)
            .get("data")

        assertEquals(announcementId, fetched.get("id").asText())
        assertEquals("DRAFT", fetched.get("status").asText())

        val updated = mockMvc.put("/announcements/$announcementId") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"Maintenance window v2","summary":null,"content":"Long body v2","priority":5}"""
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)
            .get("data")

        assertEquals("Maintenance window v2", updated.get("title").asText())
        assertEquals(5, updated.get("priority").asInt())
        assertTrue(updated.get("summary").isNull)

        mockMvc.delete("/announcements/$announcementId") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }

        mockMvc.get("/announcements/$announcementId") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `publish transition sets status, published_at and is idempotent`() {
        val token = bearer()
        val id = createDraft(token, "Release notes", "What is new", "Long body text")

        val published = mockMvc.post("/announcements/$id/publish") {
            header("Authorization", "Bearer $token")
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)
            .get("data")

        assertEquals("PUBLISHED", published.get("status").asText())
        assertNotNull(published.get("publishedAt").asText())

        val again = mockMvc.post("/announcements/$id/publish") {
            header("Authorization", "Bearer $token")
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)
            .get("data")
        assertEquals("PUBLISHED", again.get("status").asText())
    }

    @Test
    fun `archive moves published to archived and blocks further edits`() {
        val token = bearer()
        val id = createDraft(token, "Outage", null, "Outage details")
        mockMvc.post("/announcements/$id/publish") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }

        val archived = mockMvc.post("/announcements/$id/archive") {
            header("Authorization", "Bearer $token")
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)
            .get("data")
        assertEquals("ARCHIVED", archived.get("status").asText())

        val blocked = mockMvc.put("/announcements/$id") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"x","summary":null,"content":"y","priority":1}"""
        }
            .andExpect { status { isConflict() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)
        assertEquals(409, blocked.get("code").asInt())
    }

    @Test
    fun `list supports status and keyword filters`() {
        val token = bearer()
        val marker = "Outage-${UUID.randomUUID()}"
        val otherTitle = "Maintenance-${UUID.randomUUID()}"
        createDraft(token, marker, null, "Body A")
        createDraft(token, otherTitle, null, "Body B")

        val list = mockMvc.get("/announcements?status=DRAFT&keyword=${marker.substringAfter('-')}") {
            header("Authorization", "Bearer $token")
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)
            .get("data")

        assertTrue(list.isArray)
        val titles = list.map { it.get("title").asText() }
        assertTrue(titles.any { it == marker })
        assertTrue(titles.none { it == otherTitle })
    }

    @Test
    fun `validation errors use unified envelope`() {
        val token = bearer()
        val response = mockMvc.post("/announcements") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"","content":""}"""
        }
            .andExpect { status { isBadRequest() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals(400, response.get("code").asInt())
        assertNotNull(response.get("message").asText())
    }

    @Test
    fun `deleting a published announcement is rejected`() {
        val token = bearer()
        val id = createDraft(token, "Hotfix", null, "Hotfix notes")
        mockMvc.post("/announcements/$id/publish") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }

        val response = mockMvc.delete("/announcements/$id") {
            header("Authorization", "Bearer $token")
        }
            .andExpect { status { isConflict() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)
        assertEquals(409, response.get("code").asInt())

        mockMvc.get("/announcements/$id") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `mark read sets readByMe on viewer and counts once even when repeated`() {
        val token = bearer()
        val id = createDraft(token, "Read me", null, "Body content for reading")
        mockMvc.post("/announcements/$id/publish") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }

        val first = mockMvc.post("/announcements/$id/read") {
            header("Authorization", "Bearer $token")
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)
            .get("data")
        assertTrue(first.get("readByMe").asBoolean())
        assertEquals(1, first.get("readCount").asInt())

        val second = mockMvc.post("/announcements/$id/read") {
            header("Authorization", "Bearer $token")
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)
            .get("data")
        assertTrue(second.get("readByMe").asBoolean())
        assertEquals(1, second.get("readCount").asInt())
    }

    @Test
    fun `mark read on a draft is rejected`() {
        val token = bearer()
        val id = createDraft(token, "Draft only", null, "Still draft")
        val response = mockMvc.post("/announcements/$id/read") {
            header("Authorization", "Bearer $token")
        }
            .andExpect { status { isConflict() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)
        assertEquals(409, response.get("code").asInt())
    }

    @Test
    fun `list unreadOnly filter excludes announcements the viewer has read`() {
        val token = bearer()
        val readTitle = "Unread-mark-${UUID.randomUUID()}"
        val unreadTitle = "Truly-unread-${UUID.randomUUID()}"
        val readId = createDraft(token, readTitle, null, "R")
        val unreadId = createDraft(token, unreadTitle, null, "U")
        publishAs(token, readId)
        publishAs(token, unreadId)

        mockMvc.post("/announcements/$readId/read") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }

        val list = mockMvc.get("/announcements?status=PUBLISHED&unreadOnly=true") {
            header("Authorization", "Bearer $token")
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)
            .get("data")

        val titles = list.map { it.get("title").asText() }
        assertTrue(titles.contains(unreadTitle))
        assertFalse(titles.contains(readTitle))
    }

    @Test
    fun `schedule blocks publish before time and then allows publish after clearing schedule`() {
        val token = bearer()
        val id = createDraft(token, "Scheduled", null, "Future content")

        val future = LocalDateTime.now().plusHours(1).withNano(0)
        val scheduled = mockMvc.post("/announcements/$id/schedule") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"publishAt":"$future"}"""
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)
            .get("data")
        assertEquals("DRAFT", scheduled.get("status").asText())
        assertEquals(future.toString(), scheduled.get("publishAt").asText())

        val blocked = mockMvc.post("/announcements/$id/publish") {
            header("Authorization", "Bearer $token")
        }
            .andExpect { status { isConflict() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)
        assertEquals(409, blocked.get("code").asInt())

        val cleared = mockMvc.delete("/announcements/$id/schedule") {
            header("Authorization", "Bearer $token")
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)
            .get("data")
        assertTrue(cleared.get("publishAt").isNull)
        assertEquals("DRAFT", cleared.get("status").asText())

        val published = mockMvc.post("/announcements/$id/publish") {
            header("Authorization", "Bearer $token")
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)
            .get("data")
        assertEquals("PUBLISHED", published.get("status").asText())
        assertNotNull(published.get("publishedAt").asText())
    }

    @Test
    fun `schedule on a published announcement is rejected`() {
        val token = bearer()
        val id = createDraft(token, "Schedule published", null, "Body")
        publishAs(token, id)

        val response = mockMvc.post("/announcements/$id/schedule") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"publishAt":"${LocalDateTime.now().plusHours(1).withNano(0)}"}"""
        }
            .andExpect { status { isConflict() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)
        assertEquals(409, response.get("code").asInt())
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

    private fun createDraft(token: String, title: String, summary: String?, body: String): String {
        val summaryPart = if (summary == null) "\"summary\":null" else "\"summary\":\"$summary\""
        val response = mockMvc.post("/announcements") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"$title",$summaryPart,"content":"$body"}"""
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)
        return response.get("data").get("id").asText()
    }

    private fun publishAs(token: String, id: String) {
        mockMvc.post("/announcements/$id/publish") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }
    }
}
