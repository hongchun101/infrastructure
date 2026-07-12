package com.github.infrastructure.app.dictionary

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "dictionary_items")
interface DictionaryItem {
    @Id
    val id: UUID
    val categoryId: UUID
    val code: String
    val name: String
    val parentId: UUID?
    val sortOrder: Int
    val enabled: Boolean
    val createdTime: LocalDateTime
}