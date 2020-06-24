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

package com.maddyhome.idea.vim.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.toolbar.floating.AbstractFloatingToolbarProvider
import com.intellij.openapi.editor.toolbar.floating.FloatingToolbarComponent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.containers.IntArrayList
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.vimscript.VimScriptParser
import com.maddyhome.idea.vim.ui.ReloadFloatingToolbarActionGroup.Companion.ACTION_GROUP
import icons.VimIcons
import org.jetbrains.annotations.TestOnly
import java.util.regex.Pattern

/**
 * This file contains a "reload ~/.ideavimrc file" action functionality.
 * This is small floating action in the top right corner of the editor that appears if user edits configuration file.
 *
 * Here you can find:
 * - Simplified snapshot of config file
 * - Floating bar
 * - Action / action group
 */

object VimRcFileState {
  // List of hashes of non-empty trimmed lines
  // [VERSION UPDATE] 202+
  @Suppress("DEPRECATION")
  private val state = IntArrayList()

  // ModificationStamp. Can be taken only from document. Doesn't play a big role, but can help speed up [equalTo]
  private var modificationStamp = 0L

  // This is a pattern used in ideavimrc parsing for a long time. It removes all trailing/leading spaced and blank lines
  private val EOL_SPLIT_PATTERN = Pattern.compile(" *(\r\n|\n)+ *")

  var filePath: String? = null

  fun saveFileState(filePath: String, data: List<String>) {
    this.filePath = FileUtil.toSystemDependentName(filePath)

    state.clear()
    for (line in data) {
      state.add(line.hashCode())
    }
  }

  fun equalTo(document: Document): Boolean {
    val fileModificationStamp = document.modificationStamp
    if (fileModificationStamp == modificationStamp) return true

    val stateSize = state.size()
    var i = 0
    VimScriptParser.readText(document.charsSequence).forEach { line ->
      if (i >= stateSize) return false
      if (state.get(i) != line.hashCode()) return false
      i++
    }
    if (i < stateSize) return false

    modificationStamp = fileModificationStamp
    return true
  }

  @TestOnly
  fun clear() {
    state.clear()
    modificationStamp = 0
    filePath = null
  }
}

class ReloadVimRc : DumbAwareAction() {
  override fun update(e: AnActionEvent) {
    val editor = e.getData(PlatformDataKeys.EDITOR) ?: return

    // XXX: Actually, it worth to add e.presentation.description, but it doesn't work because of some reason
    val sameDoc = VimRcFileState.equalTo(editor.document)
    e.presentation.icon = if (sameDoc) VimIcons.IDEAVIM else AllIcons.Actions.BuildLoadChanges
    e.presentation.text = if (sameDoc) "No Changes" else "Reload"

    e.presentation.isEnabledAndVisible = true
  }

  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.getData(PlatformDataKeys.EDITOR) ?: return

    FileDocumentManager.getInstance().saveDocumentAsIs(editor.document)

    VimPlugin.getInstance().executeIdeaVimRc()
  }
}

class ReloadFloatingToolbar : AbstractFloatingToolbarProvider(ACTION_GROUP) {
  override val autoHideable: Boolean = false

  override val priority: Int = 0
}

class ReloadFloatingToolbarActionGroup : DefaultActionGroup() {
  override fun update(e: AnActionEvent) {
    val virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE) ?: return

    if (virtualFile.path == VimRcFileState.filePath) {
      e.getData(FloatingToolbarComponent.KEY)?.scheduleShow()
    }
  }

  companion object {
    const val ACTION_GROUP = "IdeaVim.ReloadVimRc.group"
  }
}
