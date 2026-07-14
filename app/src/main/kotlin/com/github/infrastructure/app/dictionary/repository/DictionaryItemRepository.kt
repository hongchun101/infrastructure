package com.github.infrastructure.app.dictionary.repository

import com.github.infrastructure.app.dictionary.entity.DictionaryItem
import com.github.infrastructure.app.dictionary.entity.*
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.and
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.babyfish.jimmer.sql.kt.ast.expression.isNull
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class DictionaryItemRepository(sql: KSqlClient) : AbstractKotlinRepository<DictionaryItem, UUID>(sql) {
    fun findRootByCategoryId(categoryId: UUID): List<DictionaryItem> = executeQuery {
        where(
            and(
                table.categoryId `eq?` categoryId,
                table.parentId.isNull(),
            ),
        )
        orderBy(table.sortOrder, table.code)
        select(table)
    }

    fun findByCategoryIdAndParentId(categoryId: UUID, parentId: UUID): List<DictionaryItem> = executeQuery {
        where(
            and(
                table.categoryId `eq?` categoryId,
                table.parentId `eq?` parentId,
            ),
        )
        orderBy(table.sortOrder, table.code)
        select(table)
    }

    fun countChildrenOf(parentId: UUID): Long = createQuery {
        where(and(table.parentId `eq?` parentId))
        selectCount()
    }.fetchUnlimitedCount()

    fun findParentCategoryId(itemId: UUID): UUID? = createQuery {
        where(and(table.id `eq?` itemId))
        select(table.categoryId)
    }.fetchOneOrNull()

    fun existsByCategoryIdAndCode(categoryId: UUID, code: String): Boolean = createQuery {
        where(
            and(
                table.categoryId `eq?` categoryId,
                table.code `eq?` code,
            ),
        )
        selectCount()
    }.fetchUnlimitedCount() > 0

    fun deleteByCategoryId(categoryId: UUID): Int = executeDelete {
        where(and(table.categoryId `eq?` categoryId))
    }
}
