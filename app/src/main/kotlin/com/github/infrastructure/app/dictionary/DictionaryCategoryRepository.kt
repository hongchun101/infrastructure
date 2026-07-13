package com.github.infrastructure.app.dictionary

import org.babyfish.jimmer.spring.repository.JRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.isNull
import org.babyfish.jimmer.sql.kt.toKSqlClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DictionaryCategoryRepository : JRepository<DictionaryCategory, UUID> {
    fun findByCode(code: String): DictionaryCategory? =
        toKSqlClient(sql()).createQuery(DictionaryCategory::class) {
            where(table.code eq code)
            select(table)
        }.fetchOneOrNull()

    fun findAllOrderedByCode(): List<DictionaryCategory> =
        toKSqlClient(sql()).createQuery(DictionaryCategory::class) {
            orderBy(table.code)
            select(table)
        }.execute()
}