package com.github.infrastructure.notification.entity

/**
 * Coarse-grained classification so the UI can group the inbox.
 */
object NotificationCategory {
    const val ANNOUNCEMENT = "announcement"
    const val APPROVAL = "approval"
    const val MENTION = "mention"
    const val TASK = "task"
    const val SYSTEM = "system"
}

/**
 * 0 = info (default), 1 = warning, 2 = urgent.
 */
object NotificationPriority {
    const val INFO = 0
    const val WARNING = 1
    const val URGENT = 2
}