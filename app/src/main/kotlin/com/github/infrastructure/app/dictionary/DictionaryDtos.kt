package com.github.infrastructure.app.dictionary

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class CreateDictionaryCategoryRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val code: String,
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,
    @field:Size(max = 500)
    val description: String? = null,
    val enabled: Boolean = true,
)

data class UpdateDictionaryCategoryRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,
    @field:Size(max = 500)
    val description: String?,
    val enabled: Boolean,
)

data class DictionaryCategoryResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val enabled: Boolean,
    val createdTime: LocalDateTime,
)

data class CreateDictionaryItemRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val code: String,
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,
    val parentId: UUID? = null,
    val sortOrder: Int = 0,
    val enabled: Boolean = true,
)

data class UpdateDictionaryItemRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,
    val parentId: UUID?,
    val sortOrder: Int,
    val enabled: Boolean,
)

data class DictionaryItemResponse(
    val id: UUID,
    val categoryId: UUID,
    val code: String,
    val name: String,
    val parentId: UUID?,
    val sortOrder: Int,
    val enabled: Boolean,
    val createdTime: LocalDateTime,
)