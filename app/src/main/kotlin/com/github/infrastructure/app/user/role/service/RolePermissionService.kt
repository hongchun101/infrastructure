package com.github.infrastructure.app.user.role.service

import com.github.infrastructure.app.user.entity.Permission
import com.github.infrastructure.app.user.entity.Role
import com.github.infrastructure.app.user.entity.RolePermission
import com.github.infrastructure.app.user.repository.PermissionRepository
import com.github.infrastructure.app.user.repository.RolePermissionRepository
import com.github.infrastructure.app.user.repository.RoleRepository
import com.github.infrastructure.app.user.role.CreatePermissionRequest
import com.github.infrastructure.app.user.role.CreateRoleRequest
import com.github.infrastructure.app.user.role.PermissionResponse
import com.github.infrastructure.app.user.role.PermissionSummary
import com.github.infrastructure.app.user.role.RoleResponse
import com.github.infrastructure.app.user.role.UpdateRoleRequest
import com.github.infrastructure.core.web.exception.BusinessException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/**
 * 角色与权限管理服务：负责 RBAC 资源的 CRUD 与角色-权限绑定的同步维护。
 *
 * 业务规则：
 * - 角色 `code` 全局唯一，且仅允许大写字母/数字/下划线（迁移数据已存在的 `ADMIN`/`BACKEND_OPERATOR` 满足此约束）。
 * - 删除角色前必须解除全部权限绑定；权限被引用时禁止删除，避免悬挂引用。
 * - 角色 `code` 不可修改（角色字符串是权限系统的稳定主键，已被序列化到 token / 角色权限关联中）。
 */
@Service
class RolePermissionService(
    private val roleRepository: RoleRepository,
    private val permissionRepository: PermissionRepository,
    private val rolePermissionRepository: RolePermissionRepository,
    private val clock: Clock,
) {
    fun listRoles(): List<RoleResponse> =
        roleRepository.findAllOrderedByCode().map { it.toResponse() }

    fun getRole(id: UUID): RoleResponse =
        roleRepository.findByIdWithPermissions(id)?.toResponse()
            ?: throw notFound("role")

    @Transactional
    fun createRole(request: CreateRoleRequest): RoleResponse {
        if (roleRepository.findByCode(request.code) != null) {
            throw BusinessException(HttpStatus.CONFLICT.value(), "role code already exists", HttpStatus.CONFLICT)
        }
        val now = LocalDateTime.now(clock)
        val role = roleRepository.save(
            Role {
                id = UUID.randomUUID()
                code = request.code
                name = request.name
                this.createdTime = now
            },
        ).modifiedEntity
        syncRolePermissions(role.id, request.permissionIds)
        return getRole(role.id)
    }

    @Transactional
    fun updateRole(id: UUID, request: UpdateRoleRequest): RoleResponse {
        val role = roleRepository.findById(id) ?: throw notFound("role")
        roleRepository.save(
            Role {
                this.id = role.id
                this.code = role.code
                this.name = request.name
                this.createdTime = role.createdTime
            },
        )
        syncRolePermissions(role.id, request.permissionIds)
        return getRole(role.id)
    }

    @Transactional
    fun deleteRole(id: UUID) {
        val role = roleRepository.findById(id) ?: throw notFound("role")
        if (role.rolePermissions.isNotEmpty()) {
            throw BusinessException(
                HttpStatus.CONFLICT.value(),
                "role has permissions assigned; remove them first",
                HttpStatus.CONFLICT,
            )
        }
        roleRepository.deleteById(id)
    }

    fun listPermissions(): List<PermissionResponse> =
        permissionRepository.findAllOrderedByCode().map { it.toResponse() }

    fun getPermission(id: UUID): PermissionResponse =
        permissionRepository.findById(id)?.toResponse()
            ?: throw notFound("permission")

    @Transactional
    fun createPermission(request: CreatePermissionRequest): PermissionResponse {
        if (permissionRepository.findByCode(request.code) != null) {
            throw BusinessException(HttpStatus.CONFLICT.value(), "permission code already exists", HttpStatus.CONFLICT)
        }
        val permission = permissionRepository.save(
            Permission {
                id = UUID.randomUUID()
                code = request.code
                name = request.name
            },
        ).modifiedEntity
        return permission.toResponse()
    }

    @Transactional
    fun deletePermission(id: UUID) {
        val permission = permissionRepository.findById(id) ?: throw notFound("permission")
        if (rolePermissionRepository.existsByPermissionId(id)) {
            throw BusinessException(
                HttpStatus.CONFLICT.value(),
                "permission is assigned to a role; detach before delete",
                HttpStatus.CONFLICT,
            )
        }
        permissionRepository.deleteById(id)
    }

    private fun syncRolePermissions(roleId: UUID, permissionIds: Set<UUID>) {
        val currentPermissionIds = rolePermissionRepository.findPermissionIdsByRoleId(roleId).toSet()
        val targetPermissionIds = permissionIds
        if (currentPermissionIds != targetPermissionIds) {
            rolePermissionRepository.deleteByRoleId(roleId)
            if (targetPermissionIds.isNotEmpty()) {
                validatePermissionIdsExist(targetPermissionIds)
                targetPermissionIds.forEach { permissionId ->
                    rolePermissionRepository.save(
                        RolePermission {
                            this.id = UUID.randomUUID()
                            this.roleId = roleId
                            this.permissionId = permissionId
                        },
                    )
                }
            }
        }
    }

    private fun validatePermissionIdsExist(permissionIds: Set<UUID>) {
        val existing = permissionRepository.findExistingIds(permissionIds)
        val missing = permissionIds - existing.toSet()
        if (missing.isNotEmpty()) {
            throw BusinessException(
                HttpStatus.BAD_REQUEST.value(),
                "permission ids not found: ${missing.joinToString()}",
                HttpStatus.BAD_REQUEST,
            )
        }
    }

    private fun Role.toResponse(): RoleResponse = RoleResponse(
        id = id,
        code = code,
        name = name,
        permissions = rolePermissions
            .map { PermissionSummary(it.permission.id, it.permission.code, it.permission.name) }
            .sortedBy { it.code },
        createdTime = createdTime,
    )

    private fun Permission.toResponse(): PermissionResponse = PermissionResponse(id = id, code = code, name = name)

    private fun notFound(resource: String): BusinessException =
        BusinessException(HttpStatus.NOT_FOUND.value(), "$resource not found", HttpStatus.NOT_FOUND)
}
