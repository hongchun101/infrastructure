package com.github.infrastructure.export.entity

import java.time.LocalDateTime
import java.util.UUID
import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "export_jobs")
interface ExportJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    val service: String

    @Column(name = "business_type")
    val businessType: String

    val format: String

    @Column(name = "file_name")
    val fileName: String

    val params: String?

    val status: String

    @Column(name = "total_rows")
    val totalRows: Long?

    @Column(name = "processed_rows")
    val processedRows: Long

    @Column(name = "file_id")
    val fileId: Long?

    val error: String?

    @Column(name = "owner_user_id")
    val ownerUserId: UUID?

    @Column(name = "started_at")
    val startedAt: LocalDateTime?

    @Column(name = "finished_at")
    val finishedAt: LocalDateTime?

    @Column(name = "duration_ms")
    val durationMs: Long?

    @Column(name = "expires_at")
    val expiresAt: LocalDateTime?

    @Column(name = "created_at")
    val createdAt: LocalDateTime

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime

    @Column(name = "deleted_at")
    val deletedAt: LocalDateTime?
}