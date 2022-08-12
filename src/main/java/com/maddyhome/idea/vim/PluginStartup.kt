/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.startup.StartupActivity
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.localEditors
import com.maddyhome.idea.vim.options.OptionScope

/**
 * @author Alex Plate
 */
class PluginStartup : StartupActivity.DumbAware/*, LightEditCompatible*/ {

  private var firstInitializationOccurred = false

  override fun runActivity(project: Project) {
    if (firstInitializationOccurred) return
    firstInitializationOccurred = true

    // This code should be executed once
    VimPlugin.getInstance().initialize()
  }
}

// This is a temporal workaround for VIM-2487
class PyNotebooksCloseWorkaround : ProjectManagerListener {
  override fun projectClosingBeforeSave(project: Project) {
    val close = injector.optionService.getOptionValue(OptionScope.GLOBAL, "closenotebooks").asBoolean()
    if (close) {
      localEditors().forEach { editor ->
        val virtualFile = EditorHelper.getVirtualFile(editor)
        if (virtualFile?.extension == "ipynb") {
          val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
          fileEditorManager.closeFile(virtualFile)
        }
      }
    }
  }
}
