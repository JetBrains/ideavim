/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

package com.maddyhome.idea.vim

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.maddyhome.idea.vim.listener.VimListenerManager

/**
 * @author Alex Plate
 */
class PluginStartup : StartupActivity.DumbAware/*, LightEditCompatible*/ {

  private var firstInitializationOccurred = false

  override fun runActivity(project: Project) {
    if (firstInitializationOccurred && VimPlugin.isEnabled()) {
      // This code should be executed on every project open
      // Project listeners are self-disposable, so there is no need to unregister them on project close
      VimListenerManager.ProjectListeners.add(project)
    }

    if (firstInitializationOccurred) return
    firstInitializationOccurred = true

    // This code should be executed once
    VimPlugin.getInstance().initialize()
  }
}
