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

package org.jetbrains.plugins.ideavim.config.migration

import com.maddyhome.idea.vim.config.migration.ApplicationConfigurationMigrator
import com.maddyhome.idea.vim.config.migration.ConfigMigrator
import com.maddyhome.idea.vim.config.migration.MigrationComponents
import com.maddyhome.idea.vim.config.migration.VersionDetector
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplicationConfigurationMigratorTest {
  @Test
  fun `simple migration`() {
    val migrators = UselessMigration.of(1)
    val versionDetectors = ConstantVersionDetector.of(1)
    val migrator = ApplicationConfigurationMigrator(MigrationComponents(migrators, versionDetectors, 2))

    migrator.migrate()

    assertTrue(migrators.all { it.migrationCompleted })
  }

  @Test
  fun `skip migration`() {
    val migrators = UselessMigration.of(1)
    val versionDetectors = ConstantVersionDetector.of(1)
    val migrator = ApplicationConfigurationMigrator(MigrationComponents(migrators, versionDetectors, 1))

    migrator.migrate()

    assertFalse(migrators.any { it.migrationCompleted })
  }

  @Test
  fun `multiple migrations`() {
    val migrators = UselessMigration.of(1, 2, 3)
    val versionDetectors = ConstantVersionDetector.of(1)
    val migrator = ApplicationConfigurationMigrator(MigrationComponents(migrators, versionDetectors, 4))

    migrator.migrate()

    assertTrue(migrators.all { it.migrationCompleted })
  }

  @Test
  fun `some migrators skipped`() {
    val migrators = UselessMigration.of(1, 2, 3, 4, 5)
    val versionDetectors = ConstantVersionDetector.of(1)
    val currentVersion = 4
    val migrator = ApplicationConfigurationMigrator(MigrationComponents(migrators, versionDetectors, currentVersion))

    migrator.migrate()

    val (applied, nonApplied) = migrators.partition { it.fromVersion < currentVersion }
    assertTrue(applied.all { it.migrationCompleted })
    assertFalse(nonApplied.any { it.migrationCompleted })
  }
}

private class UselessMigration(from: Int) : ConfigMigrator {
  override val fromVersion: Int = from
  override val toVersion: Int = from + 1
  var migrationCompleted = false

  override fun versionUp() {
    migrationCompleted = true
  }

  companion object {
    fun of(vararg versions: Int) = versions.map { UselessMigration(it) }.toSet()
  }
}

private class ConstantVersionDetector(private val version: Int) : VersionDetector {
  override fun extractVersion(): Int = version

  companion object {
    fun of(vararg versions: Int) = versions.map { ConstantVersionDetector(it) }
  }
}