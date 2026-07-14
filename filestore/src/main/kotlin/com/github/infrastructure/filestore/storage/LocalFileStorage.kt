package com.github.infrastructure.filestore.storage

import com.github.infrastructure.filestore.config.FilestoreProperties
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import org.slf4j.LoggerFactory

/**
 * Filesystem-backed storage. Used for local dev and single-node deployments.
 *
 * The upload/download URLs returned by this implementation point back at the
 * application's transfer endpoint; the endpoint then calls [write] / [openRead]
 * here. The contract is the same as S3-compatible providers so the rest of
 * the system is unaware of the choice.
 */
class LocalFileStorage(
    private val props: FilestoreProperties.LocalProvider,
) : FileStorage {
    override val provider: String = "local"
    private val log = LoggerFactory.getLogger(javaClass)
    private val root: Path = Paths.get(props.root).also { Files.createDirectories(it) }

    init {
        require(props.publicBaseUrl.isNotBlank()) { "local provider requires publicBaseUrl" }
        require(props.bucket.isNotBlank()) { "local provider requires bucket" }
        Files.createDirectories(root.resolve(props.bucket))
    }

    override fun presignUpload(
        bucket: String,
        key: String,
        contentType: String,
        expiresIn: Duration,
        maxSizeBytes: Long?,
    ): PresignedUpload {
        val token = LocalTransferToken.issue(bucket, key, contentType, expiresIn, write = true, props.signingKey)
        return PresignedUpload(
            url = "${props.publicBaseUrl.trimEnd('/')}/$bucket/${key}?token=$token",
            method = "PUT",
            headers = mapOf("Content-Type" to contentType),
            expiresAt = Instant.now().plus(expiresIn),
            maxSizeBytes = maxSizeBytes,
        )
    }

    override fun presignDownload(
        bucket: String,
        key: String,
        expiresIn: Duration,
    ): PresignedDownload {
        val token = LocalTransferToken.issue(bucket, key, "application/octet-stream", expiresIn, write = false, props.signingKey)
        return PresignedDownload(
            url = "${props.publicBaseUrl.trimEnd('/')}/$bucket/${key}?token=$token",
            expiresAt = Instant.now().plus(expiresIn),
        )
    }

    override fun write(
        bucket: String,
        key: String,
        contentType: String,
        input: InputStream,
        sizeHint: Long?,
    ): WriteResult {
        val target = resolve(bucket, key)
        Files.createDirectories(target.parent)
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        Files.newOutputStream(target).use { out ->
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
                out.write(buffer, 0, read)
                total += read
            }
        }
        return WriteResult(sizeBytes = total, sha256 = digest.digest().toHex())
    }

    override fun openRead(bucket: String, key: String): InputStream {
        val target = resolve(bucket, key)
        require(Files.exists(target)) { "object not found: $bucket/$key" }
        return Files.newInputStream(target)
    }

    override fun stat(bucket: String, key: String): FileStat? {
        val target = resolve(bucket, key)
        if (!Files.exists(target)) return null
        return FileStat(
            sizeBytes = Files.size(target),
            contentType = Files.probeContentType(target),
            etag = null,
        )
    }

    override fun delete(bucket: String, key: String) {
        val target = resolve(bucket, key)
        if (Files.exists(target)) {
            Files.delete(target)
        }
    }

    private fun resolve(bucket: String, key: String): Path {
        require(!key.contains("..")) { "invalid object key" }
        return root.resolve(bucket).resolve(key)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 64 * 1024
    }
}