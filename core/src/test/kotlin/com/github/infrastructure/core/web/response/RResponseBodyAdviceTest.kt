package com.github.infrastructure.core.web.response

import com.github.infrastructure.core.web.response.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootTest(classes = [RResponseBodyAdviceTest.TestApplication::class])
@AutoConfigureMockMvc
@Import(RResponseBodyAdviceTest.SampleController::class)
class RResponseBodyAdviceTest {
    @Autowired
    private lateinit var mockMvc: MockMvc
    @Test
    fun `wraps controller object response in R`() {
        mockMvc.get("/sample/object")
            .andExpect {
                status { isOk() }
                content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
                jsonPath("$.code") { value(0) }
                jsonPath("$.message") { value("success") }
                jsonPath("$.data.name") { value("alpha") }
            }
    }
    @Test
    fun `does not wrap response that is already R`() {
        mockMvc.get("/sample/wrapped")
            .andExpect {
                status { isOk() }
                content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
                jsonPath("$.code") { value(7) }
                jsonPath("$.message") { value("custom") }
                jsonPath("$.data.name") { value("beta") }
            }
    }
    @Test
    fun `omits data field for null controller response`() {
        val response = mockMvc.get("/sample/null")
            .andExpect {
                status { isOk() }
                content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
                jsonPath("$.code") { value(0) }
                jsonPath("$.message") { value("success") }
            }
            .andReturn()
            .response
            .contentAsString
        assertThat(response).doesNotContain("data")
    }
    @SpringBootApplication
    class TestApplication
    @RestController
    class SampleController {
        @GetMapping("/sample/object")
        fun objectResponse(): SampleResponse = SampleResponse("alpha")
        @GetMapping("/sample/wrapped")
        fun wrappedResponse(): R<SampleResponse> = R(7, "custom", SampleResponse("beta"))
        @GetMapping("/sample/null")
        fun nullResponse(): SampleResponse? = null
    }
    data class SampleResponse(
        val name: String,
    )
}
