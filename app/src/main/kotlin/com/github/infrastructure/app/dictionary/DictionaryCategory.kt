package com.github.infrastructure.app.dictionary

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "dictionary_categories")
interface DictionaryCategory {
    @Id
    val id: UUID
    val code: String
    val name: String
    val description: String?
    val enabled: Boolean
    val createdTime: LocalDateTime
}