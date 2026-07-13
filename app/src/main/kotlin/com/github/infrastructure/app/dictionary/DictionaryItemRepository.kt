package com.github.infrastructure.app.dictionary

import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.isNull
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class DictionaryItemRepository(sql: KSqlClient) : AbstractKotlinRepository<DictionaryItem, UUID>(sql) {
    fun findRootByCategoryId(categoryId: UUID): List<DictionaryItem> = executeQuery {
        where(table.categoryId eq categoryId)
        where(table.parentId.isNull())
        orderBy(table.sortOrder, table.code)
        select(table)
    }

    fun findByCategoryIdAndParentId(categoryId: UUID, parentId: UUID): List<DictionaryItem> = executeQuery {
        where(table.categoryId eq categoryId)
        where(table.parentId eq parentId)
        orderBy(table.sortOrder, table.code)
        select(table)
    }

    fun countChildrenOf(parentId: UUID): Long = createQuery {
        where(table.parentId eq parentId)
        selectCount()
    }.fetchUnlimitedCount()

    fun findParentCategoryId(itemId: UUID): UUID? = createQuery {
        where(table.id eq itemId)
        select(table.categoryId)
    }.fetchOneOrNull()

    fun existsByCategoryIdAndCode(categoryId: UUID, code: String): Boolean = createQuery {
        where(table.categoryId eq categoryId)
        where(table.code eq code)
        selectCount()
    }.fetchUnlimitedCount() > 0

    fun deleteByCategoryId(categoryId: UUID): Int = executeDelete {
        where(table.categoryId eq categoryId)
    }
}