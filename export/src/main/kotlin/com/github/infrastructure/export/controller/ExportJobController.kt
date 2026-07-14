package com.github.infrastructure.export.controller

import com.github.infrastructure.export.dto.CreateExportJobRequest
import com.github.infrastructure.export.dto.ExportHandlerSummary
import com.github.infrastructure.export.dto.ExportJobResponse
import com.github.infrastructure.export.handler.ExportHandlerRegistry
import com.github.infrastructure.export.service.ExportService
import com.github.infrastructure.security.context.CurrentUserContext
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/export-jobs")
class ExportJobController(
    private val service: ExportService,
    private val registry: ExportHandlerRegistry,
) {

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    fun create(@Valid @RequestBody request: CreateExportJobRequest): ExportJobResponse {
        val owner = CurrentUserContext.require().id
        return service.create(request, owner)
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun list(@RequestParam(required = false, defaultValue = "false") all: Boolean): List<ExportJobResponse> {
        val current = CurrentUserContext.get()
        val isAdmin = current?.permissions?.contains("export:admin") == true
        return service.list(current?.id, includeAll = all && isAdmin)
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun get(@PathVariable id: Long): ExportJobResponse {
        val current = CurrentUserContext.get()
        val isAdmin = current?.permissions?.contains("export:admin") == true
        return service.get(id, current?.id, isAdmin)
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    fun cancel(@PathVariable id: Long): ExportJobResponse {
        val current = CurrentUserContext.get()
        val isAdmin = current?.permissions?.contains("export:admin") == true
        return service.cancel(id, current?.id, isAdmin)
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("isAuthenticated()")
    fun retry(@PathVariable id: Long): ExportJobResponse {
        val current = CurrentUserContext.get()
        val isAdmin = current?.permissions?.contains("export:admin") == true
        return service.retry(id, current?.id, isAdmin)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun delete(@PathVariable id: Long) {
        val current = CurrentUserContext.get()
        val isAdmin = current?.permissions?.contains("export:admin") == true
        service.delete(id, current?.id, isAdmin)
    }

    @GetMapping("/types")
    @PreAuthorize("isAuthenticated()")
    fun types(): List<ExportHandlerSummary> = registry.all().map(ExportHandlerSummary::from)
}