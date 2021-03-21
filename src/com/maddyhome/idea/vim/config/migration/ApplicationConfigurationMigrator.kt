/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.config.migration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.maddyhome.idea.vim.VimPlugin

// This variable describe migrators and detectors that would be injected during production run
private val productionMigrationComponents = MigrationComponents(
  migrators = setOf(
    `Version 6 to 7 config migration`
  ),
  versionDetectors = listOf(
    `Detect versions 3, 4, 5, 6`
  ),
  currentVersion = VimPlugin.STATE_VERSION
)

// Just a collection of migrators and collectors
class MigrationComponents(
  val migrators: Set<ConfigMigrator>,
  val versionDetectors: List<VersionDetector>,
  val currentVersion: Int
) {
  val groupedMigrators: Map<Int, ConfigMigrator> = registerMigrators(migrators)

  companion object {
    fun registerMigrators(migrators: Set<ConfigMigrator>): Map<Int, ConfigMigrator> {
      return migrators.associateBy { it.fromVersion }
    }
  }
}

/**
 * Configuration migrator. Helps to migrate settings between different versions of IdeaVim.
 *
 * [versionDetectors] detect the version of stored settings. If this version is lower than [currentVersion], then
 *  [migrators] are applied to update the settings to the actual version of IdeaVIm settings.
 *
 * Keep in mind that [versionDetectors] and [migrators] should be stable. That means that if we can't perform migration,
 *   we should silently skip it instead of failure.
 */
@Service
class ApplicationConfigurationMigrator(migrationComponents: MigrationComponents) {

  @Suppress("unused", "HardCodedStringLiteral")
  constructor() : this(productionMigrationComponents)

  private val migrators = migrationComponents.groupedMigrators
  private val versionDetectors = migrationComponents.versionDetectors
  private var currentVersion = migrationComponents.currentVersion

  fun migrate() {
    val fileVersion = getCurrentVersion() ?: return
    if (fileVersion >= currentVersion) return

    performMigration(fileVersion)
  }

  private fun getCurrentVersion(): Int? {
    for (detector in versionDetectors) {
      val version = detector.extractVersion()
      if (version != null) return version
    }
    return null
  }

  private fun performMigration(startVersion: Int) {
    var version = startVersion
    while (version < currentVersion) {
      val configMigrator = migrators[version]
      version = if (configMigrator != null) {
        configMigrator.versionUp()
        configMigrator.toVersion
      } else version + 1
    }
  }

  companion object {
    @JvmStatic
    val instance: ApplicationConfigurationMigrator
      get() = ApplicationManager.getApplication().getService(ApplicationConfigurationMigrator::class.java)
  }
}
