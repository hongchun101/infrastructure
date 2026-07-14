package com.github.infrastructure.scheduler.entity

import java.time.LocalDateTime
import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "job_executions")
interface JobExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Column(name = "job_id")
    val jobId: Long

    val status: String

    val attempt: Int

    @Column(name = "trigger_type")
    val triggerType: String

    @Column(name = "scheduled_at")
    val scheduledAt: LocalDateTime

    @Column(name = "started_at")
    val startedAt: LocalDateTime?

    @Column(name = "finished_at")
    val finishedAt: LocalDateTime?

    @Column(name = "duration_ms")
    val durationMs: Long?

    val result: String?

    val error: String?

    @Column(name = "worker_id")
    val workerId: String?

    @Column(name = "next_run_at")
    val nextRunAt: LocalDateTime?

    @Column(name = "created_at")
    val createdAt: LocalDateTime
}