package com.github.infrastructure.app.dictionary

import org.babyfish.jimmer.spring.repository.JRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.isNull
import org.babyfish.jimmer.sql.kt.ast.expression.ne
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.babyfish.jimmer.sql.kt.toKSqlClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DictionaryItemRepository : JRepository<DictionaryItem, UUID> {
    fun findRootByCategoryId(categoryId: UUID): List<DictionaryItem> =
        toKSqlClient(sql()).createQuery(DictionaryItem::class) {
            where(table.categoryId eq categoryId)
            where(table.parentId.isNull())
            orderBy(table.sortOrder, table.code)
            select(table)
        }.execute()

    fun findByCategoryIdAndParentId(categoryId: UUID, parentId: UUID): List<DictionaryItem> =
        toKSqlClient(sql()).createQuery(DictionaryItem::class) {
            where(table.categoryId eq categoryId)
            where(table.parentId eq parentId)
            orderBy(table.sortOrder, table.code)
            select(table)
        }.execute()

    fun findByCategoryId(categoryId: UUID): List<DictionaryItem> =
        toKSqlClient(sql()).createQuery(DictionaryItem::class) {
            where(table.categoryId eq categoryId)
            orderBy(table.sortOrder, table.code)
            select(table)
        }.execute()

    fun countChildrenOf(parentId: UUID): Long =
        toKSqlClient(sql()).createQuery(DictionaryItem::class) {
            where(table.parentId eq parentId)
            selectCount()
        }.fetchUnlimitedCount()

    fun findParentCategoryId(itemId: UUID): UUID? =
        toKSqlClient(sql()).createQuery(DictionaryItem::class) {
            where(table.id eq itemId)
            select(table.categoryId)
        }.fetchOneOrNull()

    fun existsByCategoryIdAndCode(categoryId: UUID, code: String): Boolean =
        toKSqlClient(sql()).createQuery(DictionaryItem::class) {
            where(table.categoryId eq categoryId)
            where(table.code eq code)
            selectCount()
        }.fetchUnlimitedCount() > 0

    fun existsByCategoryIdAndCodeExcluding(categoryId: UUID, code: String, excludeItemId: UUID): Boolean =
        toKSqlClient(sql()).createQuery(DictionaryItem::class) {
            where(table.categoryId eq categoryId)
            where(table.code eq code)
            where(table.id ne excludeItemId)
            selectCount()
        }.fetchUnlimitedCount() > 0

    fun deleteByCategoryId(categoryId: UUID): Int =
        toKSqlClient(sql()).createDelete(DictionaryItem::class) {
            where(table.categoryId eq categoryId)
        }.execute().affectedRowCount

    fun deleteByIds(ids: Collection<UUID>): Int =
        toKSqlClient(sql()).createDelete(DictionaryItem::class) {
            where(table.id valueIn ids)
        }.execute().affectedRowCount
}