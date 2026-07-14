package com.github.infrastructure.filestore.controller

import com.github.infrastructure.filestore.dto.ConfirmUploadRequest
import com.github.infrastructure.filestore.dto.DownloadTokenResponse
import com.github.infrastructure.filestore.dto.FileObjectResponse
import com.github.infrastructure.filestore.dto.RequestDownloadTokenRequest
import com.github.infrastructure.filestore.dto.RequestUploadTokenRequest
import com.github.infrastructure.filestore.dto.UploadTokenResponse
import com.github.infrastructure.filestore.service.FileService
import com.github.infrastructure.security.context.CurrentUserContext
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/files")
class FileTokenController(
    private val fileService: FileService,
) {
    @PostMapping("/upload-token")
    @PreAuthorize("isAuthenticated()")
    fun requestUpload(@Valid @RequestBody request: RequestUploadTokenRequest): UploadTokenResponse {
        val owner = CurrentUserContext.get()?.id
        return fileService.requestUpload(request, owner)
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("isAuthenticated()")
    fun confirmUpload(
        @PathVariable id: Long,
        @RequestBody(required = false) request: ConfirmUploadRequest?,
    ): FileObjectResponse {
        val owner = CurrentUserContext.get()?.id
        return fileService.confirmUpload(id, owner, request ?: ConfirmUploadRequest())
    }

    @PostMapping("/{id}/download-token")
    @PreAuthorize("isAuthenticated()")
    fun requestDownload(
        @PathVariable id: Long,
        @Valid @RequestBody(required = false) request: RequestDownloadTokenRequest?,
    ): DownloadTokenResponse {
        val owner = CurrentUserContext.get()?.id
        return fileService.requestDownload(id, owner, request ?: RequestDownloadTokenRequest())
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun get(@PathVariable id: Long): FileObjectResponse = fileService.findById(id)

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun delete(@PathVariable id: Long) {
        val owner = CurrentUserContext.get()?.id
        fileService.delete(id, owner)
    }
}