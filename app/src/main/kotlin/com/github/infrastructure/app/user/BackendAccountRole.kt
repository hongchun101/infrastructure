package com.github.infrastructure.app.user

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.Table
import java.util.UUID

@Entity
@Table(name = "backend_account_roles")
interface BackendAccountRole {
    @org.babyfish.jimmer.sql.Id
    val id: UUID
    @Key
    @ManyToOne
    val account: BackendAccount

    @IdView("account")
    val accountId: UUID

    @Key
    @ManyToOne
    val role: Role

    @IdView("role")
    val roleId: UUID
}
