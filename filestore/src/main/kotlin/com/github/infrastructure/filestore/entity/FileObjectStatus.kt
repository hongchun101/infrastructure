package com.github.infrastructure.filestore.entity

object FileObjectStatus {
    const val PENDING = "PENDING"
    const val UPLOADED = "UPLOADED"
    const val INFECTED = "INFECTED"
    const val DELETED = "DELETED"
    const val FAILED = "FAILED"
}

object FileVisibility {
    const val PRIVATE = "PRIVATE"
    const val PUBLIC = "PUBLIC"
}

object FileStorageProvider {
    const val MINIO = "minio"
    const val OSS = "oss"
    const val COS = "cos"
    const val S3 = "s3"
    const val LOCAL = "local"
}