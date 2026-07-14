package com.github.infrastructure.alert.channel

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.infrastructure.alert.entity.AlertChannelType
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient

@Component
class DingTalkAlertChannel(
    private val objectMapper: ObjectMapper,
) : AlertChannel {
    override val type = AlertChannelType.DINGTALK

    private val client: RestClient = RestClient.builder()
        .requestFactory(WebhookHttpClientFactory.newRequestFactory())
        .build()

    override fun send(target: String, payload: JsonNode): ChannelSendOutcome {
        val body: ObjectNode = objectMapper.createObjectNode().apply {
            put("msgtype", "text")
            set<ObjectNode>("text", objectMapper.createObjectNode().put("content", buildText(payload)))
        }
        return sendJson(target, body)
    }

    private fun buildText(payload: JsonNode): String = buildString {
        val severity = payload.path("severity").asText("INFO")
        val ruleName = payload.path("ruleName").asText("alert")
        val summary = payload.path("summary").asText("")
        val occurrences = payload.path("occurrences").asLong()
        append("[")
        append(severity)
        append("] ")
        appendLine(ruleName)
        appendLine()
        appendLine(summary)
        appendLine()
        append("Occurrences: ")
        append(occurrences)
    }

    private fun sendJson(target: String, body: ObjectNode): ChannelSendOutcome = try {
        val response = client.post()
            .uri(target)
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(body))
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
