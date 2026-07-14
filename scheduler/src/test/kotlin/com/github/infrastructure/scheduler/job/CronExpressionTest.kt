package com.github.infrastructure.scheduler.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class CronExpressionTest {

    @Test
    fun `every minute fires at second 0`() {
        val cron = CronExpression("0 * * * * *")
        val now = LocalDateTime.of(2026, 7, 14, 10, 30, 15)
        val next = cron.nextAfter(now)
        assertThat(next).isEqualTo(LocalDateTime.of(2026, 7, 14, 10, 31, 0))
    }

    @Test
    fun `every 5 minutes matches 0 5 10 15 20 25 and so on`() {
        val cron = CronExpression("0 */5 * * * *")
        val from = LocalDateTime.of(2026, 7, 14, 10, 32, 0)
        val next = cron.nextAfter(from)
        assertThat(next).isEqualTo(LocalDateTime.of(2026, 7, 14, 10, 35, 0))
    }

    @Test
    fun `specific hour walks to next day`() {
        val cron = CronExpression("0 0 14 * * *")
        val from = LocalDateTime.of(2026, 7, 14, 15, 0, 0)
        val next = cron.nextAfter(from)
        assertThat(next).isEqualTo(LocalDateTime.of(2026, 7, 15, 14, 0, 0))
    }

    @Test
    fun `day-of-week monday only`() {
        val cron = CronExpression("0 0 9 * * 1")
        // 2026-07-14 is a Tuesday -> next Monday is 2026-07-20
        val from = LocalDateTime.of(2026, 7, 14, 10, 0, 0)
        val next = cron.nextAfter(from)
        assertThat(next.dayOfWeek.value).isEqualTo(1)
        assertThat(next).isEqualTo(LocalDateTime.of(2026, 7, 20, 9, 0, 0))
    }

    @Test
    fun `range field`() {
        val cron = CronExpression("0 0 9-12 * * *")
        val from = LocalDateTime.of(2026, 7, 14, 13, 0, 0)
        val next = cron.nextAfter(from)
        assertThat(next).isEqualTo(LocalDateTime.of(2026, 7, 15, 9, 0, 0))
    }

    @Test
    fun `comma-separated minutes`() {
        val cron = CronExpression("0 0,15,30,45 * * * *")
        val from = LocalDateTime.of(2026, 7, 14, 10, 17, 0)
        val next = cron.nextAfter(from)
        assertThat(next).isEqualTo(LocalDateTime.of(2026, 7, 14, 10, 30, 0))
    }

    @Test
    fun `rejects malformed expression`() {
        assertThrows(IllegalArgumentException::class.java) { CronExpression("0 * * * *") }
        assertThrows(IllegalArgumentException::class.java) { CronExpression("bad * * * * *") }
    }
}