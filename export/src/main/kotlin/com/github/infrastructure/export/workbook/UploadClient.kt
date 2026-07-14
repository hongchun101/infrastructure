package com.github.infrastructure.export.workbook

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Tiny HTTP client for PUTting bytes to a presigned upload URL. The filestore
 * module hands out upload URLs (S3 presign OR the local backend's transfer
 * endpoint); this client is the only HTTP code the export pipeline needs.
 */
@Component
class UploadClient {

    private val log = LoggerFactory.getLogger(javaClass)
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .version(HttpClient.Version.HTTP_1_1)
        .build()

    /**
     * Uploads [body] to [url] using [method] (default PUT). The headers in
     * [headers] are applied verbatim, so callers must include
     * Content-Type/Content-Length themselves.
     *
     * @throws UploadClientException when the response status is not 2xx.
     */
    fun put(url: String, method: String, headers: Map<String, String>, body: ByteArray) {
        val requestBuilder = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofMinutes(5))
            .method(method.uppercase(), HttpRequest.BodyPublishers.ofByteArray(body))
        headers.forEach { (k, v) -> requestBuilder.header(k, v) }

        val response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() !in 200..299) {
            val bodyText = runCatching { String(response.body(), Charsets.UTF_8) }
                .getOrDefault("<binary>")
            log.warn("upload to {} failed with {}: {}", url, response.statusCode(), bodyText.take(500))
            throw UploadClientException(
                "upload to $url failed: HTTP ${response.statusCode()}",
                response.statusCode(),
            )
        }
    }
}

class UploadClientException(message: String, val statusCode: Int) : RuntimeException(message)