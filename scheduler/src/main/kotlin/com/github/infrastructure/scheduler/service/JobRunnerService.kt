package com.github.infrastructure.scheduler.service

import com.github.infrastructure.scheduler.config.SchedulerProperties
import com.github.infrastructure.scheduler.entity.JobDefinition
import com.github.infrastructure.scheduler.entity.JobExecution
import com.github.infrastructure.scheduler.entity.JobStatus
import com.github.infrastructure.scheduler.entity.JobTriggerType
import com.github.infrastructure.scheduler.job.JobContext
import com.github.infrastructure.scheduler.job.JobHandler
import com.github.infrastructure.scheduler.job.JobRegistry
import com.github.infrastructure.scheduler.repository.JobDefinitionRepository
import com.github.infrastructure.scheduler.repository.JobExecutionRepository
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Picks up PENDING executions, takes a Redis lock, runs the registered
 * [JobHandler], records the outcome, and schedules retries on failure.
 *
 * Concurrency: the lock key is `lock:job:{code}` so multiple app instances
 * coordinate via Redis SETNX with a TTL slightly longer than the configured
 * timeout. Only the lock holder runs; everyone else drops the execution
 * and lets the next scan pick it up.
 */
@Service
class JobRunnerService(
    private val properties: SchedulerProperties,
    private val definitionRepository: JobDefinitionRepository,
    private val executionRepository: JobExecutionRepository,
    private val registry: JobRegistry,
    private val redis: StringRedisTemplate,
    private val dispatcher: JobDispatcherService,
    private val clock: Clock,
) : DisposableBean {

    private val log = LoggerFactory.getLogger(javaClass)
    private val workerId = "${java.lang.management.ManagementFactory.getRuntimeMXBean().name}-${UUID.randomUUID().toString().take(8)}"
    private val executor = Executors.newFixedThreadPool(properties.workerThreads.coerceAtLeast(1))

    @EventListener(ApplicationReadyEvent::class)
    fun onStartup() {
        // Reconcile handler definitions before the first scan tick fires.
        try {
            dispatcher.reconcileDefinitions()
        } catch (e: Exception) {
            log.warn("failed to reconcile job definitions: {}", e.message, e)
        }
    }

    @Scheduled(fixedDelayString = "\${infrastructure.scheduler.scan-interval-ms:1000}")
    fun scan() {
        if (!properties.enabled) return
        val pending = runCatching { executionRepository.listPending(properties.batchSize) }
            .onFailure { log.warn("could not list pending executions: {}", it.message) }
            .getOrDefault(emptyList())
        for (execution in pending) {
            executor.submit { runOne(execution.id) }
        }
    }

    private fun runOne(executionId: Long) {
        val ctx = runCatching { loadContext(executionId) }
            .getOrElse { err ->
                log.warn("could not load execution {}: {}", executionId, err.message)
                return
            }
        if (ctx == null) return
        val (execution, def, handler) = ctx

        val lockKey = "lock:job:${def.code}"
        val lockValue = "$workerId:${execution.id}"
        val lockTtl = Duration.ofSeconds(def.timeoutSeconds.coerceAtLeast(30).toLong() + 30)
        val acquired = runCatching {
            redis.opsForValue().setIfAbsent(lockKey, lockValue, lockTtl)
        }.getOrElse {
            log.warn("redis SETNX failed for {}: {}", lockKey, it.message)
            false
        }
        if (acquired != true) {
            log.debug("job '{}' is locked by another worker, skipping execution {}", def.code, executionId)
            return
        }

        try {
            executeWithTimeout(def, handler, execution)
        } finally {
            releaseLock(lockKey, lockValue)
        }
    }

    private fun executeWithTimeout(def: JobDefinition, handler: JobHandler, execution: JobExecution) {
        val startedAt = LocalDateTime.now(clock)
        markRunning(execution.id, startedAt)

        val ctx = JobContext(
            jobId = def.id,
            executionId = execution.id,
            attempt = execution.attempt,
            payload = def.payload,
            triggeredBy = execution.triggerType,
        )
        val future = executor.submit<Unit> { handler.handle(ctx) }
        try {
            val outcome = future.get(def.timeoutSeconds.coerceAtLeast(1).toLong(), TimeUnit.SECONDS)
            val finishedAt = LocalDateTime.now(clock)
            markSuccess(execution.id, finishedAt, startedAt, def.id)
        } catch (te: TimeoutException) {
            future.cancel(true)
            val finishedAt = LocalDateTime.now(clock)
            handleFailure(def, execution, startedAt, finishedAt, JobStatus.TIMEOUT, "execution exceeded ${def.timeoutSeconds}s")
        } catch (e: Exception) {
            val finishedAt = LocalDateTime.now(clock)
            handleFailure(def, execution, startedAt, finishedAt, JobStatus.FAILED, e.message ?: e.javaClass.simpleName)
        }
    }

    @Transactional
    protected fun loadContext(executionId: Long): Triple<JobExecution, JobDefinition, JobHandler>? {
        val execution = executionRepository.findById(executionId) ?: return null
        if (execution.status != JobStatus.PENDING) return null
        val def = definitionRepository.findById(execution.jobId) ?: return null
        if (!def.enabled) return null
        val handler = registry.get(def.code) ?: run {
            log.warn("no JobHandler registered for code '{}'", def.code)
            null
        } ?: return null
        return Triple(execution, def, handler)
    }

    @Transactional
    protected fun markRunning(executionId: Long, startedAt: LocalDateTime) {
        val execution = executionRepository.findById(executionId) ?: return
        if (execution.status != JobStatus.PENDING) return
        executionRepository.save(
            JobExecution {
                this.id = execution.id
                jobId = execution.jobId
                status = JobStatus.RUNNING
                attempt = execution.attempt
                triggerType = execution.triggerType
                scheduledAt = execution.scheduledAt
                this.startedAt = startedAt
                finishedAt = execution.finishedAt
                durationMs = execution.durationMs
                result = execution.result
                error = execution.error
                workerId = this@JobRunnerService.workerId
                nextRunAt = execution.nextRunAt
                createdAt = execution.createdAt
            },
        )
    }

    @Transactional
    protected fun markSuccess(executionId: Long, finishedAt: LocalDateTime, startedAt: LocalDateTime, jobId: Long) {
        val execution = executionRepository.findById(executionId) ?: return
        executionRepository.save(
            JobExecution {
                this.id = execution.id
                this.jobId = execution.jobId
                status = JobStatus.SUCCESS
                attempt = execution.attempt
                triggerType = execution.triggerType
                scheduledAt = execution.scheduledAt
                this.startedAt = startedAt
                this.finishedAt = finishedAt
                durationMs = java.time.Duration.between(startedAt, finishedAt).toMillis()
                result = "ok"
                error = null
                workerId = this@JobRunnerService.workerId
                nextRunAt = execution.nextRunAt
                createdAt = execution.createdAt
            },
        )
        updateDefinitionLastFinished(jobId, finishedAt)
    }

    @Transactional
    protected fun handleFailure(
        def: JobDefinition,
        execution: JobExecution,
        startedAt: LocalDateTime,
        finishedAt: LocalDateTime,
        terminalStatus: String,
        errorMessage: String,
    ) {
        val nextAttempt = execution.attempt + 1
        val shouldRetry = nextAttempt <= def.retryMaxAttempts && terminalStatus != JobStatus.TIMEOUT
        val backoffSeconds = if (shouldRetry) {
            computeBackoff(def, execution.attempt)
        } else null
        val nextRunAt = backoffSeconds?.let { finishedAt.plusSeconds(it) }
        val finalStatus = if (shouldRetry) JobStatus.PENDING else terminalStatus

        executionRepository.save(
            JobExecution {
                this.id = execution.id
                jobId = execution.jobId
                status = finalStatus
                attempt = execution.attempt
                triggerType = if (shouldRetry) JobTriggerType.RETRY else execution.triggerType
                scheduledAt = execution.scheduledAt
                this.startedAt = startedAt
                this.finishedAt = finishedAt
                durationMs = java.time.Duration.between(startedAt, finishedAt).toMillis()
                result = null
                error = errorMessage.take(4000)
                workerId = this@JobRunnerService.workerId
                this.nextRunAt = nextRunAt
                createdAt = execution.createdAt
            },
        )
        updateDefinitionLastFinished(def.id, finishedAt)

        if (shouldRetry) {
            executionRepository.save(
                JobExecution {
                    jobId = def.id
                    status = JobStatus.PENDING
                    attempt = nextAttempt
                    triggerType = JobTriggerType.RETRY
                    scheduledAt = nextRunAt!!
                    this.startedAt = null
                    this.finishedAt = null
                    durationMs = null
                    result = null
                    error = null
                    workerId = null
                    this.nextRunAt = nextRunAt
                    createdAt = finishedAt
                },
            )
        } else {
            log.error("job '{}' exhausted retries (attempt {}): {}", def.code, execution.attempt, errorMessage)
        }
    }

    @Transactional
    protected fun updateDefinitionLastFinished(jobId: Long, finishedAt: LocalDateTime) {
        val def = definitionRepository.findById(jobId) ?: return
        val next = dispatcher.computeNextRun(def, finishedAt)
        definitionRepository.save(
            JobDefinition {
                this.id = def.id
                code = def.code
                name = def.name
                description = def.description
                cron = def.cron
                fixedDelaySeconds = def.fixedDelaySeconds
                enabled = def.enabled
                retryMaxAttempts = def.retryMaxAttempts
                retryInitialBackoffSeconds = def.retryInitialBackoffSeconds
                retryMaxBackoffSeconds = def.retryMaxBackoffSeconds
                retryMultiplier = def.retryMultiplier
                timeoutSeconds = def.timeoutSeconds
                payload = def.payload
                lastFinishedAt = finishedAt
                lastRunAt = def.lastRunAt
                nextRunAt = next
                createdAt = def.createdAt
                updatedAt = finishedAt
            },
        )
    }

    private fun computeBackoff(def: JobDefinition, attempt: Int): Long {
        val raw = def.retryInitialBackoffSeconds.toDouble() *
            Math.pow(def.retryMultiplier, (attempt - 1).coerceAtLeast(0).toDouble())
        return raw.toLong().coerceAtMost(def.retryMaxBackoffSeconds)
    }

    private fun releaseLock(key: String, expectedValue: String) {
        // We don't bother with the Lua compare-and-delete: a stale lock TTL
        // (after timeout) just means another worker will skip this run for
        // a bit, which is fine because the next scan will re-enqueue.
        runCatching { redis.delete(key) }
            .onFailure { log.debug("could not release lock {}: {}", key, it.message) }
    }

    override fun destroy() {
        executor.shutdownNow()
    }
}