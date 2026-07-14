package com.github.infrastructure.alert.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.infrastructure.alert.dto.AlertRuleChannelSpec
import com.github.infrastructure.alert.dto.AlertRuleResponse
import com.github.infrastructure.alert.dto.CreateAlertRuleRequest
import com.github.infrastructure.alert.dto.UpdateAlertRuleRequest
import com.github.infrastructure.alert.entity.AlertChannelType
import com.github.infrastructure.alert.entity.AlertRule
import com.github.infrastructure.alert.entity.AlertRuleType
import com.github.infrastructure.alert.entity.Severity
import com.github.infrastructure.alert.repository.AlertRuleRepository
import com.github.infrastructure.alert.rule.AlertRuleMatcherRegistry
import com.github.infrastructure.core.web.exception.BusinessException
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AlertRuleService(
    private val ruleRepository: AlertRuleRepository,
    private val objectMapper: ObjectMapper,
    private val matcherRegistry: AlertRuleMatcherRegistry,
    private val clock: Clock,
) {
    fun list(): List<AlertRuleResponse> =
        ruleRepository.findAllEnabled().map { it.toResponse() }

    fun get(id: UUID): AlertRuleResponse =
        ruleRepository.findById(id)?.toResponse() ?: throw notFound(id)

    @Transactional
    fun create(request: CreateAlertRuleRequest): AlertRuleResponse {
        assertRuleTypeSupported(request.ruleType)
        if (ruleRepository.findByCode(request.code) != null) {
            throw conflict("alert rule code '${request.code}' already exists")
        }
        val now = LocalDateTime.now(clock)
        val saved = ruleRepository.save(
            AlertRule {
                id = UUID.randomUUID()
                code = request.code
                name = request.name
                description = request.description
                ruleType = request.ruleType.name
                severity = request.severity.name
                enabled = request.enabled
                sourceModule = request.sourceModule
                sourceAction = request.sourceAction
                config = objectMapper.writeValueAsString(request.config)
                channels = objectMapper.writeValueAsString(request.channels)
                createdTime = now
                updatedTime = now
            },
        ).modifiedEntity
        return saved.toResponse()
    }

    @Transactional
    fun update(id: UUID, request: UpdateAlertRuleRequest): AlertRuleResponse {
        val current = ruleRepository.findById(id) ?: throw notFound(id)
        val now = LocalDateTime.now(clock)
        val saved = ruleRepository.save(
            AlertRule {
                this.id = current.id
                code = current.code
                name = request.name ?: current.name
                description = request.description
                ruleType = current.ruleType
                severity = request.severity.name
                enabled = request.enabled
                sourceModule = request.sourceModule
                sourceAction = request.sourceAction
                config = objectMapper.writeValueAsString(request.config)
                channels = objectMapper.writeValueAsString(request.channels)
                createdTime = current.createdTime
                updatedTime = now
            },
        ).modifiedEntity
        return saved.toResponse()
    }

    @Transactional
    fun delete(id: UUID) {
        val current = ruleRepository.findById(id) ?: throw notFound(id)
        ruleRepository.deleteById(current.id)
    }

    private fun assertRuleTypeSupported(type: AlertRuleType) {
        if (matcherRegistry.get(type) == null) {
            throw BusinessException(
                HttpStatus.BAD_REQUEST.value(),
                "rule type ${type.name} has no registered matcher",
                HttpStatus.BAD_REQUEST,
            )
        }
    }

    private fun notFound(id: UUID): BusinessException =
        BusinessException(HttpStatus.NOT_FOUND.value(), "alert rule $id not found", HttpStatus.NOT_FOUND)

    private fun conflict(message: String): BusinessException =
        BusinessException(HttpStatus.CONFLICT.value(), message, HttpStatus.CONFLICT)

    private fun AlertRule.toResponse(): AlertRuleResponse {
        val configNode: JsonNode = objectMapper.readTree(config)
        val channelsNode = objectMapper.readTree(channels)
        val channelSpecs: List<AlertRuleChannelSpec> = if (channelsNode.isArray) {
            channelsNode.map { node ->
                AlertRuleChannelSpec(
                    type = AlertChannelType.valueOf(node.get("type").asText()),
                    target = node.get("target").asText(),
                )
            }
        } else emptyList()
        return AlertRuleResponse(
            id = id,
            code = code,
            name = name,
            description = description,
            ruleType = AlertRuleType.valueOf(ruleType),
            severity = Severity.valueOf(severity),
            enabled = enabled,
            sourceModule = sourceModule,
            sourceAction = sourceAction,
            config = configNode,
            channels = channelSpecs,
            createdTime = createdTime,
            updatedTime = updatedTime,
        )
    }
}
