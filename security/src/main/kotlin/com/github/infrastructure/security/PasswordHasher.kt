package com.github.infrastructure.security

interface PasswordHasher {
    fun matches(rawPassword: String, passwordHash: String): Boolean
}
