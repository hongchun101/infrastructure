package com.github.infrastructure.alert.dto

import com.github.infrastructure.alert.entity.AlertChannelType
import com.github.infrastructure.alert.entity.AlertRuleType
import com.github.infrastructure.alert.entity.Severity
import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class CreateAlertRuleRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val code: String,
    @field:NotBlank
    @field:Size(max = 200)
    val name: String,
    @field:Size(max = 500)
    val description: String? = null,
    @field:NotNull
    val ruleType: AlertRuleType,
    @field:NotNull
    val severity: Severity,
    val enabled: Boolean = true,
    @field:Size(max = 64)
    val sourceModule: String? = null,
    @field:Size(max = 100)
    val sourceAction: String? = null,
    @field:NotNull
    val config: JsonNode,
    @field:NotNull
    val channels: List<AlertRuleChannelSpec>,
)

data class UpdateAlertRuleRequest(
    @field:Size(max = 200)
    val name: String?,
    @field:Size(max = 500)
    val description: String?,
    @field:NotNull
    val severity: Severity,
    val enabled: Boolean,
    @field:Size(max = 64)
    val sourceModule: String?,
    @field:Size(max = 100)
    val sourceAction: String?,
    @field:NotNull
    val config: JsonNode,
    @field:NotNull
    val channels: List<AlertRuleChannelSpec>,
)

data class AlertRuleChannelSpec(
    @field:NotNull
    val type: AlertChannelType,
    @field:NotBlank
    @field:Size(max = 500)
    val target: String,
)

data class AlertRuleResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val ruleType: AlertRuleType,
    val severity: Severity,
    val enabled: Boolean,
    val sourceModule: String?,
    val sourceAction: String?,
    val config: JsonNode,
    val channels: List<AlertRuleChannelSpec>,
    val createdTime: LocalDateTime,
    val updatedTime: LocalDateTime,
)
