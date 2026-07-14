package com.github.infrastructure.alert.channel

import com.fasterxml.jackson.databind.JsonNode
import com.github.infrastructure.alert.entity.AlertChannelType
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient

@Component
class WebhookAlertChannel(
    private val objectMapper: com.fasterxml.jackson.databind.ObjectMapper,
) : AlertChannel {
    override val type = AlertChannelType.WEBHOOK

    private val log = LoggerFactory.getLogger(javaClass)
    private val client: RestClient = RestClient.builder()
        .requestFactory(buildRequestFactory())
        .build()

    override fun send(target: String, payload: JsonNode): ChannelSendOutcome {
        val body = objectMapper.writeValueAsString(payload)
        return try {
            val response = client.post()
                .uri(target)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.USER_AGENT, "infrastructure-alert-webhook/1.0")
                .body(body)
                .retrieve()
                .toBodilessEntity()
            ChannelSendOutcome(httpStatus = response.statusCode.value(), errorMessage = null)
        } catch (e: HttpClientErrorException) {
            ChannelSendOutcome(httpStatus = e.statusCode.value(), errorMessage = e.responseBodyAsString.take(2000))
        } catch (e: HttpServerErrorException) {
            ChannelSendOutcome(httpStatus = e.statusCode.value(), errorMessage = e.responseBodyAsString.take(2000))
        } catch (e: Exception) {
            ChannelSendOutcome(httpStatus = null, errorMessage = e.message?.take(2000))
        }
    }

    private fun buildRequestFactory(): SimpleClientHttpRequestFactory = try {
        val factory = SimpleClientHttpRequestFactory()
        factory.setConnectTimeout(5000)
        factory.setReadTimeout(5000)
        factory
    } catch (e: Exception) {
        log.warn("failed to build request factory, falling back to default", e)
        SimpleClientHttpRequestFactory()
    }
}
