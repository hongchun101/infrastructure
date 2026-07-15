package com.github.infrastructure.app.user.role.controller

import com.github.infrastructure.app.user.role.CreatePermissionRequest
import com.github.infrastructure.app.user.role.CreateRoleRequest
import com.github.infrastructure.app.user.role.PermissionResponse
import com.github.infrastructure.app.user.role.RoleResponse
import com.github.infrastructure.app.user.role.UpdateRoleRequest
import com.github.infrastructure.app.user.role.service.RolePermissionService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class RolePermissionController(
    private val rolePermissionService: RolePermissionService,
) {
    @GetMapping("/roles")
    @PreAuthorize("@permissionChecker.has('role:read')")
    fun listRoles(): List<RoleResponse> = rolePermissionService.listRoles()

    @GetMapping("/roles/{id}")
    @PreAuthorize("@permissionChecker.has('role:read')")
    fun getRole(@PathVariable id: UUID): RoleResponse = rolePermissionService.getRole(id)

    @PostMapping("/roles")
    @PreAuthorize("@permissionChecker.has('role:write')")
    fun createRole(@Valid @RequestBody request: CreateRoleRequest): RoleResponse =
        rolePermissionService.createRole(request)

    @PutMapping("/roles/{id}")
    @PreAuthorize("@permissionChecker.has('role:write')")
    fun updateRole(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateRoleRequest,
    ): RoleResponse = rolePermissionService.updateRole(id, request)

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("@permissionChecker.has('role:write')")
    fun deleteRole(@PathVariable id: UUID) {
        rolePermissionService.deleteRole(id)
    }

    @GetMapping("/permissions")
    @PreAuthorize("@permissionChecker.has('permission:read')")
    fun listPermissions(): List<PermissionResponse> = rolePermissionService.listPermissions()

    @GetMapping("/permissions/{id}")
    @PreAuthorize("@permissionChecker.has('permission:read')")
    fun getPermission(@PathVariable id: UUID): PermissionResponse =
        rolePermissionService.getPermission(id)

    @PostMapping("/permissions")
    @PreAuthorize("@permissionChecker.has('permission:write')")
    fun createPermission(@Valid @RequestBody request: CreatePermissionRequest): PermissionResponse =
        rolePermissionService.createPermission(request)

    @DeleteMapping("/permissions/{id}")
    @PreAuthorize("@permissionChecker.has('permission:write')")
    fun deletePermission(@PathVariable id: UUID) {
        rolePermissionService.deletePermission(id)
    }
}
