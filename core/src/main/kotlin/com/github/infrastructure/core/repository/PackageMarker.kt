package com.github.infrastructure.core.repository

/**
 * Marker package for the `repository` layer of the core module.
 *
 * The core module provides cross-cutting web utilities (response envelope,
 * exception handling, web auto-configuration) and does not currently expose
 * any persistence repositories of its own. This file keeps the package
 * present so the module layout stays consistent with the other modules.
 */
internal object CoreRepositoryPackageMarker
