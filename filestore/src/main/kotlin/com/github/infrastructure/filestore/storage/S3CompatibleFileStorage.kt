package com.github.infrastructure.filestore.storage

import com.github.infrastructure.filestore.config.FilestoreProperties
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.StatObjectArgs
import io.minio.StatObjectResponse
import io.minio.http.Method
import java.io.InputStream
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import org.slf4j.LoggerFactory

/**
 * Generic S3-compatible backend. The MinIO client speaks the AWS S3 v4
 * signature dialect and works against MinIO, AWS S3, Aliyun OSS (with the
 * OSS S3-compatible endpoint), Tencent COS, Wasabi, Backblaze B2, etc.
 *
 * Pass [FilestoreProperties.Provider] configuration with the matching
 * endpoint + bucket + access key/secret.
 */
class S3CompatibleFileStorage(
    private val props: FilestoreProperties.S3Provider,
    override val provider: String,
    private val client: MinioClient = MinioClient.builder()
        .endpoint(props.endpoint)
        .credentials(props.accessKey, props.secretKey)
        .region(props.region)
        .build(),
) : FileStorage {

    private val log = LoggerFactory.getLogger(javaClass)

    init {
        require(props.bucket.isNotBlank()) { "S3-compatible provider requires bucket" }
    }

    override fun presignUpload(
        bucket: String,
        key: String,
        contentType: String,
        expiresIn: Duration,
        maxSizeBytes: Long?,
    ): PresignedUpload {
        val expirySeconds = expiresIn.toSeconds().toInt().coerceAtLeast(60)
        val url = client.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.PUT)
                .bucket(bucket)
                .`object`(key)
                .expiry(expirySeconds)
                .build(),
        )
        val headers = buildMap {
            put("Content-Type", contentType)
            maxSizeBytes?.let { put("Content-Length", it.toString()) }
        }
        return PresignedUpload(
            url = applyPublicEndpoint(url),
            headers = headers,
            expiresAt = Instant.now().plus(expiresIn),
            maxSizeBytes = maxSizeBytes,
        )
    }

    override fun presignDownload(
        bucket: String,
        key: String,
        expiresIn: Duration,
    ): PresignedDownload {
        val expirySeconds = expiresIn.toSeconds().toInt().coerceAtLeast(60)
        val url = client.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .`object`(key)
                .expiry(expirySeconds)
                .build(),
        )
        return PresignedDownload(
            url = applyPublicEndpoint(url),
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
        // Used only by confirm() when the client didn't go directly to the
        // object store. Stream through MinIO while computing sha256.
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        val tmp = java.io.File.createTempFile("filestore-", ".tmp")
        tmp.outputStream().use { out ->
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
                out.write(buffer, 0, read)
                total += read
            }
        }
        try {
            client.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(key)
                    .contentType(contentType)
                    .stream(tmp.inputStream(), total, -1)
                    .build(),
            )
        } finally {
            tmp.delete()
        }
        return WriteResult(sizeBytes = total, sha256 = digest.digest().toHex())
    }

    override fun openRead(bucket: String, key: String): InputStream =
        client.getObject(
            io.minio.GetObjectArgs.builder().bucket(bucket).`object`(key).build(),
        )

    override fun stat(bucket: String, key: String): FileStat? = try {
        val resp: StatObjectResponse = client.statObject(
            StatObjectArgs.builder().bucket(bucket).`object`(key).build(),
        )
        FileStat(
            sizeBytes = resp.size(),
            contentType = resp.contentType(),
            etag = resp.etag(),
        )
    } catch (e: io.minio.errors.ErrorResponseException) {
        if (e.errorResponse().code() == "NoSuchKey") null else throw e
    }

    override fun delete(bucket: String, key: String) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).`object`(key).build())
        } catch (e: io.minio.errors.ErrorResponseException) {
            if (e.errorResponse().code() != "NoSuchKey") {
                log.warn("failed to delete {} from {}: {}", key, bucket, e.message)
                throw e
            }
        }
    }

    private fun applyPublicEndpoint(internalUrl: String): String {
        val publicEndpoint = props.publicEndpoint?.takeIf { it.isNotBlank() } ?: return internalUrl
        // Replace the scheme+host of the MinIO-signed URL with the public endpoint.
        // Path stays the same so the signature remains valid for the same object key.
        val signed = java.net.URI(internalUrl)
        val public = java.net.URI(publicEndpoint)
        val sb = StringBuilder()
        public.scheme?.let { sb.append(it).append("://") }
        public.host?.let { sb.append(it) }
        if (public.port != -1) sb.append(":").append(public.port)
        signed.rawPath?.let { sb.append(it) }
        signed.rawQuery?.let { sb.append("?").append(it) }
        return sb.toString()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 64 * 1024
    }
}