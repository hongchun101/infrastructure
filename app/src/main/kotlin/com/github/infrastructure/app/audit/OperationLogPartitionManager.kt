package com.github.infrastructure.app.audit

import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Order(Int.MIN_VALUE)
class OperationLogPartitionManager(
    private val jdbcClient: JdbcClient,
    private val clock: Clock,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)
    private val nameFormatter = DateTimeFormatter.ofPattern("yyyyMM")

    override fun run(args: ApplicationArguments?) {
        ensurePartitions(monthsAhead = 1)
    }

    @Scheduled(cron = "0 30 0 1 * *")
    fun ensureMonthlyPartitions() {
        ensurePartitions(monthsAhead = 1)
    }

    fun ensurePartitions(monthsAhead: Int) {
        val now = LocalDate.now(clock)
        for (offset in 0..monthsAhead) {
            val target = now.plusMonths(offset.toLong())
            val partitionName = "operation_logs_${target.format(nameFormatter)}"
            if (partitionExists(partitionName)) continue
            val start = target.withDayOfMonth(1)
            val end = start.plusMonths(1)
            try {
                jdbcClient.sql(
                    """
                    create table $partitionName
                    partition of operation_logs
                    for values from (:start) to (:end)
                    """.trimIndent(),
                )
                    .param("start", start)
                    .param("end", end)
                    .update()
                log.info("created operation log partition: {}", partitionName)
            } catch (e: DataAccessException) {
                log.debug("operation log partition {} not created: {}", partitionName, e.message)
            } catch (e: Exception) {
                log.warn("failed to create operation log partition {}", partitionName, e)
            }
        }
    }

    private fun partitionExists(name: String): Boolean = try {
        jdbcClient.sql(
            """
            select 1 from information_schema.tables where table_name = :name
            """.trimIndent(),
        )
            .param("name", name)
            .query(Int::class.java)
            .optional()
            .isPresent
    } catch (e: Exception) {
        log.debug("partition existence check failed for {}", name, e)
        false
    }
}