package com.github.infrastructure.filestore.controller

import com.github.infrastructure.filestore.config.FilestoreProperties
import com.github.infrastructure.filestore.error.FileErrors
import com.github.infrastructure.filestore.storage.LocalTransferToken
import com.github.infrastructure.filestore.storage.StorageRegistry
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Local-storage transfer endpoint. Only used when the active [FileStorage] is
 * the local backend (single-node deployments, dev). For S3-compatible
 * providers the client PUTs/GETs the presigned URL directly to the object
 * service and this controller is never hit.
 *
 * The route shape mirrors the S3 presigned URL: `/{bucket}/{key}` with
 * `?token=` for HMAC verification.
 */
@RestController
@RequestMapping("/api/files/transfer")
class FileTransferController(
    private val registry: StorageRegistry,
    private val props: FilestoreProperties,
) {
    @PutMapping("/{bucket}/**", consumes = ["*/*"])
    fun put(
        @PathVariable bucket: String,
        @RequestParam("token") token: String,
        @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) contentType: String?,
        request: HttpServletRequest,
    ): ResponseEntity<Map<String, Any>> {
        val key = extractKey(request, bucket)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "missing object key"))
        val storage = registry.resolve(bucket)
        if (storage.provider != "local") {
            return ResponseEntity.status(405).body(mapOf("error" to "provider '${storage.provider}' does not accept streamed writes through this endpoint"))
        }
        val verification = LocalTransferToken.verify(
            token = token,
            expectedBucket = bucket,
            expectedKey = key,
            expectedOp = "PUT",
            signingKey = props.local.signingKey,
        )
        if (verification is LocalTransferToken.Result.Invalid) {
            throw FileErrors.transferForbidden(verification.reason)
        }
        val verified = verification as LocalTransferToken.Result.Ok
        val result = storage.write(
            bucket = bucket,
            key = key,
            contentType = contentType ?: verified.contentType,
            input = request.inputStream,
            sizeHint = request.contentLengthLong.takeIf { it > 0 },
        )
        return ResponseEntity.ok(
            mapOf(
                "bucket" to bucket,
                "key" to key,
                "sizeBytes" to result.sizeBytes,
                "sha256" to result.sha256,
            ),
        )
    }

    @GetMapping("/{bucket}/**")
    fun get(
        @PathVariable bucket: String,
        @RequestParam("token") token: String,
        request: HttpServletRequest,
    ): ResponseEntity<InputStreamResource> {
        val key = extractKey(request, bucket)
            ?: return ResponseEntity.badRequest().build()
        val storage = registry.resolve(bucket)
        if (storage.provider != "local") {
            return ResponseEntity.status(405).build()
        }
        val verification = LocalTransferToken.verify(
            token = token,
            expectedBucket = bucket,
            expectedKey = key,
            expectedOp = "GET",
            signingKey = props.local.signingKey,
        )
        if (verification is LocalTransferToken.Result.Invalid) {
            throw FileErrors.transferForbidden(verification.reason)
        }
        val stat = storage.stat(bucket, key)
            ?: return ResponseEntity.notFound().build()
        val stream = storage.openRead(bucket, key)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(stat.contentType ?: "application/octet-stream"))
            .contentLength(stat.sizeBytes)
            .body(InputStreamResource(stream))
    }

    private fun extractKey(request: HttpServletRequest, bucket: String): String? {
        val path = request.requestURI.substringAfter("/api/files/transfer/$bucket/").trimStart('/')
        if (path.isBlank()) return null
        return java.net.URLDecoder.decode(path, Charsets.UTF_8)
    }
}