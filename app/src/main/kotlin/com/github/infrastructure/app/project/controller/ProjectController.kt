package com.github.infrastructure.app.project.controller

import com.github.infrastructure.app.project.CreateProjectRequest
import com.github.infrastructure.app.project.ProjectResponse
import com.github.infrastructure.app.project.service.ProjectService
import com.github.infrastructure.security.context.CurrentUserContext
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ProjectController(
    private val projectService: ProjectService,
) {
    @GetMapping("/projects")
    @PreAuthorize("@permissionChecker.has('project:read')")
    fun list(): List<ProjectResponse> = projectService.list(CurrentUserContext.require())

    @PostMapping("/projects")
    @PreAuthorize("@permissionChecker.has('project:write')")
    fun create(@Valid @RequestBody request: CreateProjectRequest): ProjectResponse =
        projectService.create(request, CurrentUserContext.require())

    @GetMapping("/projects/{id}")
    @PreAuthorize("@permissionChecker.has('project:read')")
    fun get(@PathVariable id: UUID): ProjectResponse = projectService.get(id, CurrentUserContext.require())
}
