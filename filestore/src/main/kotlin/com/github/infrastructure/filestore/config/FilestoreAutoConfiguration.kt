package com.github.infrastructure.filestore.config

import com.github.infrastructure.filestore.storage.FileStorage
import com.github.infrastructure.filestore.storage.LocalFileStorage
import com.github.infrastructure.filestore.storage.S3CompatibleFileStorage
import com.github.infrastructure.filestore.storage.StorageRegistry
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@EnableConfigurationProperties(FilestoreProperties::class)
class FilestoreAutoConfiguration {

    @Bean
    fun defaultFileStorage(props: FilestoreProperties): FileStorage {
        return when (props.defaultProvider.lowercase()) {
            "local" -> LocalFileStorage(props.local)
            "minio", "oss", "cos", "s3" -> {
                val p = props.providerConfig(props.defaultProvider)
                require(p.enabled) { "${props.defaultProvider} provider is not enabled" }
                S3CompatibleFileStorage(p, props.defaultProvider.lowercase())
            }
            else -> throw IllegalArgumentException("unsupported file storage provider: ${props.defaultProvider}")
        }
    }

    @Bean
    fun storageRegistry(default: FileStorage): StorageRegistry = StorageRegistry(default)
}