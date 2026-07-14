package com.github.infrastructure.scheduler.entity

import java.time.LocalDateTime
import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "job_definitions")
interface JobDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    val code: String

    val name: String

    val description: String?

    val cron: String?

    @Column(name = "fixed_delay_seconds")
    val fixedDelaySeconds: Int?

    val enabled: Boolean

    @Column(name = "retry_max_attempts")
    val retryMaxAttempts: Int

    @Column(name = "retry_initial_backoff_seconds")
    val retryInitialBackoffSeconds: Long

    @Column(name = "retry_max_backoff_seconds")
    val retryMaxBackoffSeconds: Long

    @Column(name = "retry_multiplier")
    val retryMultiplier: Double

    @Column(name = "timeout_seconds")
    val timeoutSeconds: Int

    val payload: String?

    @Column(name = "last_finished_at")
    val lastFinishedAt: LocalDateTime?

    @Column(name = "last_run_at")
    val lastRunAt: LocalDateTime?

    @Column(name = "next_run_at")
    val nextRunAt: LocalDateTime?

    @Column(name = "created_at")
    val createdAt: LocalDateTime

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime
}