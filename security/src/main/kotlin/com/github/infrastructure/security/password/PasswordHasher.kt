package com.github.infrastructure.security.password

interface PasswordHasher {
    fun matches(rawPassword: String, passwordHash: String): Boolean
}
