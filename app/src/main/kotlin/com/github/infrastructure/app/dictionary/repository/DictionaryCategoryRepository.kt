package com.github.infrastructure.app.dictionary.repository

import com.github.infrastructure.app.dictionary.DictionaryCategory
import com.github.infrastructure.app.dictionary.*
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class DictionaryCategoryRepository(sql: KSqlClient) : AbstractKotlinRepository<DictionaryCategory, UUID>(sql) {
    fun findByCode(code: String): DictionaryCategory? = createQuery {
        where(table.code `eq?` code)
        select(table)
    }.fetchOneOrNull()

    fun findAllOrderedByCode(): List<DictionaryCategory> = executeQuery {
        orderBy(table.code)
        select(table)
    }
}
