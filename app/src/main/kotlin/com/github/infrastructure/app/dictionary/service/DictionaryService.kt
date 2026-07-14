package com.github.infrastructure.app.dictionary.service

import com.github.infrastructure.app.dictionary.dto.CreateDictionaryCategoryRequest
import com.github.infrastructure.app.dictionary.dto.CreateDictionaryItemRequest
import com.github.infrastructure.app.dictionary.dto.DictionaryCategoryResponse
import com.github.infrastructure.app.dictionary.dto.DictionaryItemResponse
import com.github.infrastructure.app.dictionary.dto.UpdateDictionaryCategoryRequest
import com.github.infrastructure.app.dictionary.dto.UpdateDictionaryItemRequest
import com.github.infrastructure.app.dictionary.entity.DictionaryCategory
import com.github.infrastructure.app.dictionary.entity.DictionaryItem
import com.github.infrastructure.app.dictionary.repository.DictionaryCategoryRepository
import com.github.infrastructure.app.dictionary.repository.DictionaryItemRepository
import com.github.infrastructure.core.web.exception.BusinessException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class DictionaryService(
    private val categoryRepository: DictionaryCategoryRepository,
    private val itemRepository: DictionaryItemRepository,
    private val clock: Clock,
) {
    @Transactional
    fun createCategory(request: CreateDictionaryCategoryRequest): DictionaryCategoryResponse {
        if (categoryRepository.findByCode(request.code) != null) {
            throw BusinessException(HttpStatus.CONFLICT.value(), "category code already exists", HttpStatus.CONFLICT)
        }
        val createdTime = LocalDateTime.now(clock)
        val category = categoryRepository.save(
            DictionaryCategory {
                id = UUID.randomUUID()
                code = request.code
                name = request.name
                description = request.description
                enabled = request.enabled
                this.createdTime = createdTime
            },
        ).modifiedEntity
        return category.toResponse()
    }

    fun listCategories(): List<DictionaryCategoryResponse> =
        categoryRepository.findAllOrderedByCode().map { it.toResponse() }

    fun getCategory(id: UUID): DictionaryCategoryResponse =
        categoryRepository.findById(id)?.toResponse()
            ?: throw notFound("category")

    fun getCategoryByCode(code: String): DictionaryCategoryResponse =
        categoryRepository.findByCode(code)?.toResponse()
            ?: throw notFound("category")

    @Transactional
    fun updateCategory(id: UUID, request: UpdateDictionaryCategoryRequest): DictionaryCategoryResponse {
        val current = categoryRepository.findById(id) ?: throw notFound("category")
        val updated = categoryRepository.save(
            DictionaryCategory {
                this.id = current.id
                code = current.code
                name = request.name
                description = request.description
                enabled = request.enabled
                createdTime = current.createdTime
            },
        ).modifiedEntity
        return updated.toResponse()
    }

    @Transactional
    fun deleteCategory(id: UUID) {
        if (categoryRepository.findById(id) == null) throw notFound("category")
        itemRepository.deleteByCategoryId(id)
        categoryRepository.deleteById(id)
    }

    @Transactional
    fun createItem(categoryCode: String, request: CreateDictionaryItemRequest): DictionaryItemResponse {
        val category = categoryRepository.findByCode(categoryCode) ?: throw notFound("category")
        request.parentId?.let { validateParent(request.parentId, category.id) }
        if (itemRepository.existsByCategoryIdAndCode(category.id, request.code)) {
            throw BusinessException(HttpStatus.CONFLICT.value(), "item code already exists in category", HttpStatus.CONFLICT)
        }
        val createdTime = LocalDateTime.now(clock)
        val item = itemRepository.save(
            DictionaryItem {
                id = UUID.randomUUID()
                categoryId = category.id
                code = request.code
                name = request.name
                parentId = request.parentId
                sortOrder = request.sortOrder
                enabled = request.enabled
                this.createdTime = createdTime
            },
        ).modifiedEntity
        return item.toResponse()
    }

    fun listItems(categoryCode: String, parentId: UUID?): List<DictionaryItemResponse> {
        val category = categoryRepository.findByCode(categoryCode) ?: throw notFound("category")
        val items = if (parentId == null) {
            itemRepository.findRootByCategoryId(category.id)
        } else {
            itemRepository.findByCategoryIdAndParentId(category.id, parentId)
        }
        return items.map { it.toResponse() }
    }

    @Transactional
    fun updateItem(itemId: UUID, request: UpdateDictionaryItemRequest): DictionaryItemResponse {
        val current = itemRepository.findById(itemId) ?: throw notFound("item")
        request.parentId?.let { validateParent(it, current.categoryId, excludeItemId = current.id) }
        val updated = itemRepository.save(
            DictionaryItem {
                id = current.id
                categoryId = current.categoryId
                code = current.code
                name = request.name
                parentId = request.parentId
                sortOrder = request.sortOrder
                enabled = request.enabled
                createdTime = current.createdTime
            },
        ).modifiedEntity
        return updated.toResponse()
    }

    @Transactional
    fun deleteItem(itemId: UUID) {
        val item = itemRepository.findById(itemId) ?: throw notFound("item")
        if (itemRepository.countChildrenOf(item.id) > 0) {
            throw BusinessException(
                HttpStatus.CONFLICT.value(),
                "item has child entries; delete children first",
                HttpStatus.CONFLICT,
            )
        }
        itemRepository.deleteById(item.id)
    }

    private fun validateParent(parentId: UUID, expectedCategoryId: UUID, excludeItemId: UUID? = null) {
        val parentCategoryId = itemRepository.findParentCategoryId(parentId)
            ?: throw BusinessException(
                HttpStatus.BAD_REQUEST.value(),
                "parent item not found",
                HttpStatus.BAD_REQUEST,
            )
        if (parentCategoryId != expectedCategoryId) {
            throw BusinessException(
                HttpStatus.BAD_REQUEST.value(),
                "parent item belongs to a different category",
                HttpStatus.BAD_REQUEST,
            )
        }
        if (excludeItemId != null && parentId == excludeItemId) {
            throw BusinessException(
                HttpStatus.BAD_REQUEST.value(),
                "item cannot be its own parent",
                HttpStatus.BAD_REQUEST,
            )
        }
    }

    private fun notFound(resource: String): BusinessException =
        BusinessException(HttpStatus.NOT_FOUND.value(), "$resource not found", HttpStatus.NOT_FOUND)

    private fun DictionaryCategory.toResponse(): DictionaryCategoryResponse = DictionaryCategoryResponse(
        id = id,
        code = code,
        name = name,
        description = description,
        enabled = enabled,
        createdTime = createdTime,
    )

    private fun DictionaryItem.toResponse(): DictionaryItemResponse = DictionaryItemResponse(
        id = id,
        categoryId = categoryId,
        code = code,
        name = name,
        parentId = parentId,
        sortOrder = sortOrder,
        enabled = enabled,
        createdTime = createdTime,
    )
}
