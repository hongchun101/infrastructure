package com.github.infrastructure.app.dictionary.cache

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.infrastructure.app.dictionary.dto.DictionaryCategoryResponse
import com.github.infrastructure.app.dictionary.dto.DictionaryItemResponse
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

/**
 * 字典缓存：按 code 与 id 缓存 `category`/`item`，写操作（create/update/delete）触发失效。
 *
 * 字典是高频读、极少写的配置数据；Redis 缓存可让 `/dictionaries/{code}` 与按 category 列出 item 的接口从
 * 每次访问数据库降为一次 Redis 命中。写路径仍直连 DB（数据正确性优先），但写完会显式失效对应键，保证
 * 读多写少场景下的一致性。
 */
@Component
class DictionaryCache(
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getCategoryByCode(code: String): DictionaryCategoryResponse? = readJson(categoryCodeKey(code), CATEGORY_TYPE)

    fun putCategoryByCode(code: String, value: DictionaryCategoryResponse) {
        writeJson(categoryCodeKey(code), value, CATEGORY_TTL)
    }

    fun getCategoryById(id: UUID): DictionaryCategoryResponse? = readJson(categoryIdKey(id), CATEGORY_TYPE)

    fun putCategoryById(id: UUID, value: DictionaryCategoryResponse) {
        writeJson(categoryIdKey(id), value, CATEGORY_TTL)
    }

    fun getItemsByCategory(categoryCode: String, parentId: UUID?): List<DictionaryItemResponse>? =
        readJson(itemsKey(categoryCode, parentId), ITEMS_TYPE)

    fun putItemsByCategory(categoryCode: String, parentId: UUID?, value: List<DictionaryItemResponse>) {
        writeJson(itemsKey(categoryCode, parentId), value, ITEMS_TTL)
    }

    fun invalidateCategory(code: String, id: UUID) {
        runCatching { redis.delete(categoryCodeKey(code)) }
            .onFailure { log.debug("failed to delete dictionary cache for code {}: {}", code, it.message) }
        runCatching { redis.delete(categoryIdKey(id)) }
            .onFailure { log.debug("failed to delete dictionary cache for id {}: {}", id, it.message) }
    }

    fun invalidateCategoryItems(categoryCode: String) {
        val pattern = "dictionary:items:$categoryCode:*"
        try {
            val keys = redis.keys(pattern)
            if (keys.isNotEmpty()) redis.delete(keys)
        } catch (e: Exception) {
            log.debug("failed to delete dictionary items cache for category {}: {}", categoryCode, e.message)
        }
    }

    fun invalidateAllForCategory(code: String, id: UUID) {
        invalidateCategory(code, id)
        invalidateCategoryItems(code)
    }

    fun invalidateItem(categoryCode: String) {
        invalidateCategoryItems(categoryCode)
    }

    private fun categoryCodeKey(code: String): String = "dictionary:category:code:$code"
    private fun categoryIdKey(id: UUID): String = "dictionary:category:id:$id"
    private fun itemsKey(code: String, parentId: UUID?): String =
        if (parentId == null) "dictionary:items:$code:root" else "dictionary:items:$code:parent:$parentId"

    private fun <T> readJson(key: String, type: TypeReference<T>): T? = try {
        val raw = redis.opsForValue().get(key) ?: return null
        objectMapper.readValue(raw, type)
    } catch (e: Exception) {
        log.debug("dictionary cache read miss for {}: {}", key, e.message)
        null
    }

    private fun writeJson(key: String, value: Any, ttl: Duration) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl)
        } catch (e: Exception) {
            log.debug("dictionary cache write failed for {}: {}", key, e.message)
        }
    }

    companion object {
        private val CATEGORY_TYPE = object : TypeReference<DictionaryCategoryResponse>() {}
        private val ITEMS_TYPE = object : TypeReference<List<DictionaryItemResponse>>() {}
        private val CATEGORY_TTL: Duration = Duration.ofMinutes(10)
        private val ITEMS_TTL: Duration = Duration.ofMinutes(5)
    }
}
