package com.github.infrastructure.app.dictionary.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.JoinColumn
import org.babyfish.jimmer.sql.LogicalDeleted
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.OnDissociate
import org.babyfish.jimmer.sql.DissociateAction
import org.babyfish.jimmer.sql.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "dictionary_items")
interface DictionaryItem {
    @Id
    val id: UUID

    @ManyToOne
    @JoinColumn(name = "category_id")
    @OnDissociate(DissociateAction.DELETE)
    val category: DictionaryCategory

    @IdView("category")
    val categoryId: UUID

    val code: String
    val name: String

    @ManyToOne
    @JoinColumn(name = "parent_id")
    @OnDissociate(DissociateAction.CHECK)
    val parent: DictionaryItem?

    @IdView("parent")
    val parentId: UUID?

    val sortOrder: Int
    val enabled: Boolean
    val createdTime: LocalDateTime

    @LogicalDeleted("now")
    val deletedAt: LocalDateTime?
}
