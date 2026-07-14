package com.github.infrastructure.filestore.storage

/**
 * Resolves the active [FileStorage] implementation for a given bucket name.
 * One provider covers all buckets by default; multi-provider deployments can
 * be added later by extending [FilestoreProperties] with bucket-to-provider
 * mapping.
 */
class StorageRegistry(
    private val default: FileStorage,
) {
    fun resolve(bucket: String): FileStorage = default

    fun provider(): String = default.provider
}