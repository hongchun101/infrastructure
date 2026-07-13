package com.github.infrastructure.app.announcement

import com.github.infrastructure.app.user.User
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.JoinColumn
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.OnDissociate
import org.babyfish.jimmer.sql.DissociateAction
import org.babyfish.jimmer.sql.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "announcement_reads")
interface AnnouncementRead {
    @org.babyfish.jimmer.sql.Id
    val id: UUID
    @Key
    @ManyToOne
    @JoinColumn(name = "announcement_id")
    @OnDissociate(DissociateAction.DELETE)
    val announcement: Announcement

    @IdView("announcement")
    val announcementId: UUID

    @Key
    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: User

    @IdView("user")
    val userId: UUID

    val readAt: LocalDateTime
}