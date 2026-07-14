package com.github.infrastructure.alert.repository

import com.github.infrastructure.alert.entity.AlertNotification
import com.github.infrastructure.alert.entity.*
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class AlertNotificationRepository(sql: KSqlClient) : AbstractKotlinRepository<AlertNotification, UUID>(sql) {
    fun findPageByEventId(
        eventId: UUID,
        pageIndex: Int,
        pageSize: Int,
    ): Page<AlertNotification> = createQuery {
        where(table.eventId `eq?` eventId)
        orderBy(table.sentAt.desc())
        select(table)
    }.fetchPage(pageIndex, pageSize)
}
