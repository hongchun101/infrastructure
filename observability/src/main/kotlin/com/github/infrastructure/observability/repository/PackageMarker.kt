package com.github.infrastructure.observability.repository

/**
 * Marker package for the `repository` layer of the observability module.
 *
 * The observability module ships the trace-id filter and auto-configuration
 * and does not currently expose any persistence repositories of its own.
 * This file keeps the package present so the module layout stays
 * consistent with the other modules.
 */
internal object ObservabilityRepositoryPackageMarker
