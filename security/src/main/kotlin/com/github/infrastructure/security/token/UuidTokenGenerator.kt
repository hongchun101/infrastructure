package com.github.infrastructure.security.token

import java.util.UUID

class UuidTokenGenerator(
    private val uuid: () -> String = { UUID.randomUUID().toString() },
) {
    fun next(): String = uuid()
}
