package com.github.infrastructure.core.web

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest(classes = [GlobalExceptionHandlerTest.TestApplication::class])
@AutoConfigureMockMvc
@Import(GlobalExceptionHandlerTest.SampleController::class)
class GlobalExceptionHandlerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `maps business exception to response status code and message`() {
        mockMvc.get("/errors/not-found")
            .andExpect {
                status { isNotFound() }
                content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
                jsonPath("$.code") { value(404) }
                jsonPath("$.message") { value("project not found") }
                jsonPath("$.data") { doesNotExist() }
            }
    }

    @Test
    fun `maps custom business exception code without changing response status`() {
        mockMvc.get("/errors/business-code")
            .andExpect {
                status { isConflict() }
                content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
                jsonPath("$.code") { value(10001) }
                jsonPath("$.message") { value("project archived") }
                jsonPath("$.data") { doesNotExist() }
            }
    }

    @Test
    fun `maps request body validation failure to bad request`() {
        mockMvc.post("/errors/validated") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":""}"""
        }.andExpect {
            status { isBadRequest() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value(400) }
            jsonPath("$.message") { value("name must not be blank") }
            jsonPath("$.data") { doesNotExist() }
        }
    }

    @Test
    fun `maps missing request parameter to bad request`() {
        mockMvc.get("/errors/required-param")
            .andExpect {
                status { isBadRequest() }
                content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
                jsonPath("$.code") { value(400) }
                jsonPath("$.message") { value("missing required parameter: name") }
                jsonPath("$.data") { doesNotExist() }
            }
    }

    @Test
    fun `maps path variable type mismatch to bad request`() {
        mockMvc.get("/errors/type-mismatch/not-a-number")
            .andExpect {
                status { isBadRequest() }
                content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
                jsonPath("$.code") { value(400) }
                jsonPath("$.message") { value("invalid value for parameter: id") }
                jsonPath("$.data") { doesNotExist() }
            }
    }

    @Test
    fun `maps malformed json request body to bad request`() {
        mockMvc.post("/errors/validated") {
            contentType = MediaType.APPLICATION_JSON
            content = "{"
        }.andExpect {
            status { isBadRequest() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value(400) }
            jsonPath("$.message") { value("malformed request body") }
            jsonPath("$.data") { doesNotExist() }
        }
    }

    @Test
    fun `maps unsupported request method to method not allowed`() {
        mockMvc.post("/errors/not-found")
            .andExpect {
                status { isMethodNotAllowed() }
                content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
                jsonPath("$.code") { value(405) }
                jsonPath("$.message") { value("request method not supported") }
                jsonPath("$.data") { doesNotExist() }
            }
    }

    @Test
    fun `maps response status exception to response status and reason`() {
        mockMvc.get("/errors/response-status")
            .andExpect {
                status { isForbidden() }
                content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
                jsonPath("$.code") { value(403) }
                jsonPath("$.message") { value("access denied") }
                jsonPath("$.data") { doesNotExist() }
            }
    }

    @Test
    fun `maps unhandled exception to safe internal server error`() {
        mockMvc.get("/errors/unhandled")
            .andExpect {
                status { isInternalServerError() }
                content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
                jsonPath("$.code") { value(500) }
                jsonPath("$.message") { value("internal server error") }
                jsonPath("$.data") { doesNotExist() }
            }
    }

    @SpringBootApplication
    class TestApplication

    @RestController
    class SampleController {
        @GetMapping("/errors/not-found")
        fun notFound(): String = throw NotFoundException("project not found")

        @GetMapping("/errors/business-code")
        fun businessCode(): String = throw BusinessException(10001, "project archived", HttpStatus.CONFLICT)

        @PostMapping("/errors/validated")
        fun validated(@Valid @RequestBody request: CreateRequest): String = request.name

        @GetMapping("/errors/required-param")
        fun requiredParam(@RequestParam name: String): String = name

        @GetMapping("/errors/type-mismatch/{id}")
        fun typeMismatch(@PathVariable id: Long): Long = id

        @GetMapping("/errors/response-status")
        fun responseStatus(): String = throw ResponseStatusException(HttpStatus.FORBIDDEN, "access denied")

        @GetMapping("/errors/unhandled")
        fun unhandled(): String = throw IllegalStateException("database password leaked")
    }

    data class CreateRequest(
        @field:NotBlank
        val name: String,
    )
}
