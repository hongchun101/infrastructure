package com.github.infrastructure.app.dictionary.service

import com.github.infrastructure.app.dictionary.cache.DictionaryCache
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
    private val cache: DictionaryCache,
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
        val response = category.toResponse()
        cache.putCategoryByCode(response.code, response)
        cache.putCategoryById(response.id, response)
        return response
    }

    fun listCategories(): List<DictionaryCategoryResponse> =
        categoryRepository.findAllOrderedByCode().map { it.toResponse() }

    fun getCategory(id: UUID): DictionaryCategoryResponse {
        cache.getCategoryById(id)?.let { return it }
        val response = categoryRepository.findById(id)?.toResponse() ?: throw notFound("category")
        cache.putCategoryById(id, response)
        return response
    }

    fun getCategoryByCode(code: String): DictionaryCategoryResponse {
        cache.getCategoryByCode(code)?.let { return it }
        val response = categoryRepository.findByCode(code)?.toResponse() ?: throw notFound("category")
        cache.putCategoryByCode(code, response)
        return response
    }

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
        val response = updated.toResponse()
        cache.invalidateAllForCategory(current.code, current.id)
        cache.putCategoryByCode(response.code, response)
        cache.putCategoryById(response.id, response)
        return response
    }
    @Transactional
    fun deleteCategory(id: UUID) {
        val existing = categoryRepository.findById(id) ?: throw notFound("category")
        itemRepository.deleteByCategoryId(id)
        categoryRepository.deleteById(id)
        cache.invalidateAllForCategory(existing.code, existing.id)
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
        cache.invalidateItem(categoryCode)
        return item.toResponse()
    }

    fun listItems(categoryCode: String, parentId: UUID?): List<DictionaryItemResponse> {
        cache.getItemsByCategory(categoryCode, parentId)?.let { return it }
        val category = categoryRepository.findByCode(categoryCode) ?: throw notFound("category")
        val items = if (parentId == null) {
            itemRepository.findRootByCategoryId(category.id)
        } else {
            itemRepository.findByCategoryIdAndParentId(category.id, parentId)
        }
        val responses = items.map { it.toResponse() }
        cache.putItemsByCategory(categoryCode, parentId, responses)
        return responses
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
        val categoryCode = categoryRepository.findById(current.categoryId)?.code
        if (categoryCode != null) cache.invalidateItem(categoryCode)
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
        val categoryCode = categoryRepository.findById(item.categoryId)?.code
        itemRepository.deleteById(item.id)
        if (categoryCode != null) cache.invalidateItem(categoryCode)
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
