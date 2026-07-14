package com.github.infrastructure.filestore.entity

import java.time.LocalDateTime
import java.util.UUID
import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "file_objects")
interface FileObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Column(name = "biz_type")
    val bizType: String

    @Column(name = "biz_id")
    val bizId: String?

    val bucket: String

    @Column(name = "object_key")
    val objectKey: String

    @Column(name = "original_name")
    val originalName: String

    @Column(name = "content_type")
    val contentType: String

    @Column(name = "size_bytes")
    val sizeBytes: Long?

    val sha256: String?

    @Column(name = "storage_provider")
    val storageProvider: String

    val visibility: String

    val status: String

    @Column(name = "owner_user_id")
    val ownerUserId: UUID?

    val metadata: String?

    @Column(name = "created_at")
    val createdAt: LocalDateTime

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime

    @Column(name = "uploaded_at")
    val uploadedAt: LocalDateTime?

    @Column(name = "expires_at")
    val expiresAt: LocalDateTime?

    @Column(name = "deleted_at")
    val deletedAt: LocalDateTime?
}