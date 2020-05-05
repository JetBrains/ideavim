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
import com.intellij.util.containers.IntArrayList
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.vimscript.VimScriptParser
import com.maddyhome.idea.vim.ui.ReloadFloatingToolbarActionGroup.Companion.ACTION_GROUP
import icons.VimIcons
import java.io.File

object VimRcFileState {
  private val state = IntArrayList()
  private var modificationStamp = 0L
  var fileName = ".ideavimrc"

  fun saveFile(file: File) {
    fileName = file.name

    val data = VimScriptParser.readFile(file)
    hash(data)
  }

  private fun hash(charSequence: CharSequence) {
    state.clear()
    for (line in VimScriptParser.EOL_SPLIT_PATTERN.split(charSequence)) {
      if (line.isBlank()) continue
      state.add(line.hashCode())
    }
  }

  fun equalTo(file: Document): Boolean {
    if (file.modificationStamp == modificationStamp) return true

    var i = 0
    for (line in VimScriptParser.EOL_SPLIT_PATTERN.split(file.charsSequence)) {
      if (line.isBlank()) continue
      if (state.get(i) != line.hashCode()) return false
      i++
    }
    modificationStamp = file.modificationStamp
    return true
  }
}

class ReloadVimRc : DumbAwareAction() {
  override fun update(e: AnActionEvent) {
    val editor = e.getData(PlatformDataKeys.EDITOR) ?: return

    // XXX: Actually, it worth to add e.presentation.description, but it doesn't work because of some reason
    e.presentation.icon = if (VimRcFileState.equalTo(editor.document)) VimIcons.IDEAVIM else AllIcons.Actions.BuildLoadChanges
    e.presentation.text = if (VimRcFileState.equalTo(editor.document)) "No Changes" else "Reload"

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
    val toolbarComponent = e.getData(FloatingToolbarComponent.KEY) ?: return

    val virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE) ?: return

    if (virtualFile.name == VimRcFileState.fileName) {
      toolbarComponent.scheduleShow()
    }
  }

  companion object {
    const val ACTION_GROUP = "IdeaVim.ReloadVimRc.group"
  }
}
