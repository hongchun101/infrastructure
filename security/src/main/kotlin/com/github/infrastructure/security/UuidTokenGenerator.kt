package com.github.infrastructure.security

import java.util.UUID

class UuidTokenGenerator(
    private val uuid: () -> String = { UUID.randomUUID().toString() },
) {
    fun next(): String = uuid()
}
