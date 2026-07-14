package com.github.infrastructure.scheduler.job

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Lightweight 6-field cron parser (seconds minutes hours day-of-month month day-of-week).
 * We avoid pulling in quartz so the module stays minimal. Sufficient for our
 * fixed-delay and minute-granularity use cases; full cron DSL is out of scope.
 *
 * Expression: `sec min hour dom mon dow`, e.g. `0 star-slash-5 * * * *` (every 5 min).
 * Supports `*`, `step-N`, `a-b`, `a,b,c`, single value. Year is ignored.
 */
class CronExpression(expression: String) {

    private val fields: List<CronField>

    init {
        val parts = expression.trim().split(Regex("\\s+"))
        require(parts.size == 6) { "expected 6-field cron (sec min hour dom mon dow), got '$expression'" }
        fields = listOf(
            CronField(parts[0], 0, 59),
            CronField(parts[1], 0, 59),
            CronField(parts[2], 0, 23),
            CronField(parts[3], 1, 31),
            CronField(parts[4], 1, 12),
            CronField(parts[5], 0, 6),
        )
    }

    /**
     * Returns the next fire time strictly after [from].
     */
    fun nextAfter(from: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): LocalDateTime {
        var candidate = from.plusSeconds(1).withNano(0)
        // bound the search; for pathological expressions we accept a few seconds of overshoot
        repeat(366 * 24 * 60) {
            if (matches(candidate)) return candidate
            candidate = candidate.plusSeconds(1)
        }
        throw IllegalStateException("no matching cron time within 1y for $this")
    }

    private fun matches(t: LocalDateTime): Boolean {
        return fields[0].matches(t.second) &&
            fields[1].matches(t.minute) &&
            fields[2].matches(t.hour) &&
            fields[3].matches(t.dayOfMonth) &&
            fields[4].matches(t.monthValue) &&
            fields[5].matches(t.dayOfWeek.value % 7)
    }

    override fun toString(): String = fields.joinToString(" ") { it.toString() }

    private class CronField(spec: String, private val min: Int, private val max: Int) {
        private val values: Set<Int> = parse(spec)

        fun matches(v: Int): Boolean = v in values

        override fun toString(): String = values.sorted().joinToString(",")

        private fun parse(spec: String): Set<Int> {
            val result = mutableSetOf<Int>()
            spec.split(',').forEach { piece ->
                when {
                    piece == "*" -> result += min..max
                    piece.startsWith("*/") -> {
                        val step = piece.substring(2).toInt()
                        require(step > 0) { "step must be > 0" }
                        var v = min
                        while (v <= max) { result += v; v += step }
                    }
                    '-' in piece -> {
                        val (a, b) = piece.split('-').map { it.toInt() }
                        result += a..b
                    }
                    else -> result += piece.toInt()
                }
            }
            require(result.isNotEmpty()) { "cron field produced no values: '$spec'" }
            result.forEach { require(it in min..max) { "value $it out of range [$min, $max]" } }
            return result
        }
    }
}