package com.github.infrastructure.filestore.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties(prefix = "infrastructure.filestore")
class FilestoreProperties {

    /**
     * Which provider to use for the default bucket. One of `minio`, `oss`,
     * `cos`, `s3`, `local`. (All S3-compatible providers share the same
     * underlying client — only the `endpoint` differs.)
     */
    var defaultProvider: String = "local"

    /**
     * Bucket that receives uploads when the caller doesn't specify one.
     */
    var defaultBucket: String = "default"

    @NestedConfigurationProperty
    var upload: Upload = Upload()

    @NestedConfigurationProperty
    var download: Download = Download()

    @NestedConfigurationProperty
    var local: LocalProvider = LocalProvider()

    @NestedConfigurationProperty
    var minio: S3Provider = S3Provider()

    @NestedConfigurationProperty
    var oss: S3Provider = S3Provider()

    @NestedConfigurationProperty
    var cos: S3Provider = S3Provider()

    @NestedConfigurationProperty
    var s3: S3Provider = S3Provider()

    fun providerConfig(provider: String): S3Provider = when (provider.lowercase()) {
        "minio" -> minio
        "oss" -> oss
        "cos" -> cos
        "s3" -> s3
        else -> throw IllegalArgumentException("provider '$provider' has no nested config")
    }

    class Upload {
        var maxSizeBytes: Long = 50L * 1024 * 1024
        var defaultTtlSeconds: Long = 3600
        var maxTtlSeconds: Long = 24 * 3600
        var allowedContentTypes: List<String> = emptyList()
    }

    class Download {
        var defaultTtlSeconds: Long = 300
        var maxTtlSeconds: Long = 3600
    }

    class LocalProvider {
        var root: String = "./data/filestore"
        var publicBaseUrl: String = "http://localhost:8080/api/files/transfer"
        var bucket: String = "default"
        var signingKey: String = "change-me-please"
    }

    class S3Provider {
        var enabled: Boolean = false
        var endpoint: String = ""
        var publicEndpoint: String = ""
        var region: String = "us-east-1"
        var accessKey: String = ""
        var secretKey: String = ""
        var bucket: String = ""
        var pathStyle: Boolean = false
    }
}