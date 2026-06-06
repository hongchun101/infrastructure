package com.github.infrastructure.security

interface SecurityUserAccountRepository {
    fun findByUsername(username: String): SecurityUserAccount?
}
