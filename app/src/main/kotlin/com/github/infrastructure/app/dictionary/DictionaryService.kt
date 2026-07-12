package com.github.infrastructure.app.dictionary

import com.github.infrastructure.core.web.exception.BusinessException
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class DictionaryService(
    private val jdbcClient: JdbcClient,
    private val clock: Clock,
) {
    @Transactional
    fun createCategory(request: CreateDictionaryCategoryRequest): DictionaryCategoryResponse {
        val existing = jdbcClient.sql("select id from dictionary_categories where code = :code")
            .param("code", request.code)
            .query(UUID::class.java)
            .optional()
        if (existing.isPresent) {
            throw BusinessException(HttpStatus.CONFLICT.value(), "category code already exists", HttpStatus.CONFLICT)
        }
        val id = UUID.randomUUID()
        val createdTime = LocalDateTime.now(clock)
        jdbcClient.sql(
            """
            insert into dictionary_categories (id, code, name, description, enabled, created_time)
            values (:id, :code, :name, :description, :enabled, :createdTime)
            """.trimIndent(),
        )
            .param("id", id)
            .param("code", request.code)
            .param("name", request.name)
            .param("description", request.description)
            .param("enabled", request.enabled)
            .param("createdTime", createdTime)
            .update()
        return DictionaryCategoryResponse(id, request.code, request.name, request.description, request.enabled, createdTime)
    }

    fun listCategories(): List<DictionaryCategoryResponse> = jdbcClient.sql(
        """
        select id, code, name, description, enabled, created_time
        from dictionary_categories
        order by code
        """.trimIndent(),
    )
        .query(::mapCategory)
        .list()

    fun getCategory(id: UUID): DictionaryCategoryResponse = jdbcClient.sql(
        """
        select id, code, name, description, enabled, created_time
        from dictionary_categories
        where id = :id
        """.trimIndent(),
    )
        .param("id", id)
        .query(::mapCategory)
        .optional()
        .orElseThrow { notFound("category") }

    fun getCategoryByCode(code: String): DictionaryCategoryResponse = jdbcClient.sql(
        """
        select id, code, name, description, enabled, created_time
        from dictionary_categories
        where code = :code
        """.trimIndent(),
    )
        .param("code", code)
        .query(::mapCategory)
        .optional()
        .orElseThrow { notFound("category") }

    @Transactional
    fun updateCategory(id: UUID, request: UpdateDictionaryCategoryRequest): DictionaryCategoryResponse {
        val updated = jdbcClient.sql(
            """
            update dictionary_categories
            set name = :name, description = :description, enabled = :enabled
            where id = :id
            """.trimIndent(),
        )
            .param("id", id)
            .param("name", request.name)
            .param("description", request.description)
            .param("enabled", request.enabled)
            .update()
        if (updated == 0) {
            throw notFound("category")
        }
        return getCategory(id)
    }

    @Transactional
    fun deleteCategory(id: UUID) {
        val deleted = jdbcClient.sql("delete from dictionary_categories where id = :id")
            .param("id", id)
            .update()
        if (deleted == 0) {
            throw notFound("category")
        }
    }

    @Transactional
    fun createItem(categoryCode: String, request: CreateDictionaryItemRequest): DictionaryItemResponse {
        val category = getCategoryByCode(categoryCode)
        request.parentId?.let { validateParent(request.parentId, category.id) }
        if (itemCodeExists(category.id, request.code)) {
            throw BusinessException(HttpStatus.CONFLICT.value(), "item code already exists in category", HttpStatus.CONFLICT)
        }
        val id = UUID.randomUUID()
        val createdTime = LocalDateTime.now(clock)
        jdbcClient.sql(
            """
            insert into dictionary_items (id, category_id, code, name, parent_id, sort_order, enabled, created_time)
            values (:id, :categoryId, :code, :name, :parentId, :sortOrder, :enabled, :createdTime)
            """.trimIndent(),
        )
            .param("id", id)
            .param("categoryId", category.id)
            .param("code", request.code)
            .param("name", request.name)
            .param("parentId", request.parentId)
            .param("sortOrder", request.sortOrder)
            .param("enabled", request.enabled)
            .param("createdTime", createdTime)
            .update()
        return DictionaryItemResponse(
            id = id,
            categoryId = category.id,
            code = request.code,
            name = request.name,
            parentId = request.parentId,
            sortOrder = request.sortOrder,
            enabled = request.enabled,
            createdTime = createdTime,
        )
    }

    fun listItems(categoryCode: String, parentId: UUID?): List<DictionaryItemResponse> {
        val category = getCategoryByCode(categoryCode)
        val sql = if (parentId == null) {
            """
            select id, category_id, code, name, parent_id, sort_order, enabled, created_time
            from dictionary_items
            where category_id = :categoryId and parent_id is null
            order by sort_order, code
            """.trimIndent()
        } else {
            """
            select id, category_id, code, name, parent_id, sort_order, enabled, created_time
            from dictionary_items
            where category_id = :categoryId and parent_id = :parentId
            order by sort_order, code
            """.trimIndent()
        }
        val query = jdbcClient.sql(sql).param("categoryId", category.id)
        if (parentId != null) query.param("parentId", parentId)
        return query.query(::mapItem).list()
    }

    @Transactional
    fun updateItem(itemId: UUID, request: UpdateDictionaryItemRequest): DictionaryItemResponse {
        val item = getItemOrThrow(itemId)
        request.parentId?.let { validateParent(it, item.categoryId, excludeItemId = item.id) }
        val updated = jdbcClient.sql(
            """
            update dictionary_items
            set name = :name, parent_id = :parentId, sort_order = :sortOrder, enabled = :enabled
            where id = :id
            """.trimIndent(),
        )
            .param("id", itemId)
            .param("name", request.name)
            .param("parentId", request.parentId)
            .param("sortOrder", request.sortOrder)
            .param("enabled", request.enabled)
            .update()
        if (updated == 0) {
            throw notFound("item")
        }
        return getItemOrThrow(itemId)
    }

    @Transactional
    fun deleteItem(itemId: UUID) {
        val item = getItemOrThrow(itemId)
        val children = jdbcClient.sql("select count(*) from dictionary_items where parent_id = :parentId")
            .param("parentId", item.id)
            .query(Int::class.java)
            .single()
        if (children > 0) {
            throw BusinessException(
                HttpStatus.CONFLICT.value(),
                "item has child entries; delete children first",
                HttpStatus.CONFLICT,
            )
        }
        jdbcClient.sql("delete from dictionary_items where id = :id")
            .param("id", itemId)
            .update()
    }

    private fun getItemOrThrow(itemId: UUID): DictionaryItemResponse = jdbcClient.sql(
        """
        select id, category_id, code, name, parent_id, sort_order, enabled, created_time
        from dictionary_items
        where id = :id
        """.trimIndent(),
    )
        .param("id", itemId)
        .query(::mapItem)
        .optional()
        .orElseThrow { notFound("item") }

    private fun validateParent(parentId: UUID, expectedCategoryId: UUID, excludeItemId: UUID? = null) {
        val parent = jdbcClient.sql(
            """
            select category_id from dictionary_items where id = :id
            """.trimIndent(),
        )
            .param("id", parentId)
            .query(UUID::class.java)
            .optional()
            .orElseThrow {
                BusinessException(
                    HttpStatus.BAD_REQUEST.value(),
                    "parent item not found",
                    HttpStatus.BAD_REQUEST,
                )
            }
        if (parent != expectedCategoryId) {
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

    private fun itemCodeExists(categoryId: UUID, code: String): Boolean = jdbcClient.sql(
        """
        select 1 from dictionary_items where category_id = :categoryId and code = :code
        """.trimIndent(),
    )
        .param("categoryId", categoryId)
        .param("code", code)
        .query(Int::class.java)
        .optional()
        .isPresent

    private fun notFound(resource: String): BusinessException =
        BusinessException(HttpStatus.NOT_FOUND.value(), "$resource not found", HttpStatus.NOT_FOUND)

    private fun mapCategory(rs: ResultSet, rowNumber: Int): DictionaryCategoryResponse = DictionaryCategoryResponse(
        id = rs.getObject("id", UUID::class.java),
        code = rs.getString("code"),
        name = rs.getString("name"),
        description = rs.getString("description"),
        enabled = rs.getBoolean("enabled"),
        createdTime = rs.getTimestamp("created_time").toLocalDateTime(),
    )

    private fun mapItem(rs: ResultSet, rowNumber: Int): DictionaryItemResponse = DictionaryItemResponse(
        id = rs.getObject("id", UUID::class.java),
        categoryId = rs.getObject("category_id", UUID::class.java),
        code = rs.getString("code"),
        name = rs.getString("name"),
        parentId = rs.getObject("parent_id", UUID::class.java),
        sortOrder = rs.getInt("sort_order"),
        enabled = rs.getBoolean("enabled"),
        createdTime = rs.getTimestamp("created_time").toLocalDateTime(),
    )
}