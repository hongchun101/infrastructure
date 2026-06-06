package com.github.infrastructure.app.user

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.util.UUID

@Entity
@Table(name = "permissions")
interface Permission {
    @Id
    val id: UUID
    val code: String
    val name: String
}
