package com.github.infrastructure.app.dictionary.controller

import com.github.infrastructure.app.audit.annotation.OperationLog
import com.github.infrastructure.app.dictionary.dto.CreateDictionaryCategoryRequest
import com.github.infrastructure.app.dictionary.dto.CreateDictionaryItemRequest
import com.github.infrastructure.app.dictionary.dto.DictionaryCategoryResponse
import com.github.infrastructure.app.dictionary.dto.DictionaryItemResponse
import com.github.infrastructure.app.dictionary.dto.UpdateDictionaryCategoryRequest
import com.github.infrastructure.app.dictionary.dto.UpdateDictionaryItemRequest
import com.github.infrastructure.app.dictionary.service.DictionaryService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class DictionaryController(
    private val dictionaryService: DictionaryService,
) {
    @GetMapping("/dictionaries")
    fun listCategories(): List<DictionaryCategoryResponse> = dictionaryService.listCategories()

    @GetMapping("/dictionaries/{code}")
    fun getCategory(@PathVariable code: String): DictionaryCategoryResponse =
        dictionaryService.getCategoryByCode(code)

    @PostMapping("/dictionaries")
    @OperationLog(module = "dictionary", action = "create-category", description = "Create dictionary category")
    fun createCategory(@Valid @RequestBody request: CreateDictionaryCategoryRequest): DictionaryCategoryResponse =
        dictionaryService.createCategory(request)

    @PutMapping("/dictionaries/{id}")
    fun updateCategory(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateDictionaryCategoryRequest,
    ): DictionaryCategoryResponse = dictionaryService.updateCategory(id, request)
    @DeleteMapping("/dictionaries/{id}")
    @OperationLog(module = "dictionary", action = "delete-category", description = "Delete dictionary category")
    fun deleteCategory(@PathVariable id: UUID) {
        dictionaryService.deleteCategory(id)
    }
    @GetMapping("/dictionaries/{code}/items")
    fun listItems(
        @PathVariable code: String,
        @RequestParam(required = false) parentId: UUID?,
    ): List<DictionaryItemResponse> = dictionaryService.listItems(code, parentId)

    @PostMapping("/dictionaries/{code}/items")
    fun createItem(
        @PathVariable code: String,
        @Valid @RequestBody request: CreateDictionaryItemRequest,
    ): DictionaryItemResponse = dictionaryService.createItem(code, request)

    @PutMapping("/dictionaries/items/{itemId}")
    fun updateItem(
        @PathVariable itemId: UUID,
        @Valid @RequestBody request: UpdateDictionaryItemRequest,
    ): DictionaryItemResponse = dictionaryService.updateItem(itemId, request)

    @DeleteMapping("/dictionaries/items/{itemId}")
    fun deleteItem(@PathVariable itemId: UUID) {
        dictionaryService.deleteItem(itemId)
    }
}
