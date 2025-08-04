/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.scripting.ide

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

fun Path.getFiles(extension: String) =
  Files.walk(this)
    .toList()
    .filter { path -> path.endsWith(extension) }
    .map { path -> path.toFile() }

@Service
class DependenciesProviderService {
  fun collectDependencies(pluginId: String): List<File> {
    val loadedPlugin = PluginManagerCore.loadedPlugins
      .firstOrNull { it.isEnabled && it.pluginId.idString == pluginId } ?: return emptyList()
    return loadedPlugin.pluginPath?.getFiles(".jar") ?: emptyList()
  }

  fun getClassLoader(pluginId: String): ClassLoader? {
    val loadedPlugin = PluginManagerCore.loadedPlugins
      .firstOrNull { it.isEnabled && it.pluginId.idString == pluginId } ?: return null
    return loadedPlugin.classLoader
  }

  companion object {
    fun instance(): DependenciesProviderService = service<DependenciesProviderService>()
  }
}