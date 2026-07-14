package com.github.infrastructure.scheduler.entity

object JobStatus {
    const val PENDING = "PENDING"
    const val RUNNING = "RUNNING"
    const val SUCCESS = "SUCCESS"
    const val FAILED = "FAILED"
    const val TIMEOUT = "TIMEOUT"
}

object JobTriggerType {
    const val SCHEDULED = "SCHEDULED"
    const val MANUAL = "MANUAL"
    const val RETRY = "RETRY"
}

object ScheduleType {
    const val CRON = "CRON"
    const val FIXED_DELAY = "FIXED_DELAY"
}