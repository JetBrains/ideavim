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

package com.maddyhome.idea.vim.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.toolbar.floating.AbstractFloatingToolbarProvider
import com.intellij.openapi.editor.toolbar.floating.FloatingToolbarComponent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.ui.ReloadFloatingToolbarActionGroup.Companion.ACTION_GROUP
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import com.maddyhome.idea.vim.vimscript.services.VimRcService
import com.maddyhome.idea.vim.vimscript.services.VimRcService.executeIdeaVimRc
import icons.VimIcons
import org.jetbrains.annotations.TestOnly

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
  // Hash of .ideavimrc parsed to Script class
  private var state: Int? = null

  // ModificationStamp. Can be taken only from document. Doesn't play a big role, but can help speed up [equalTo]
  private var modificationStamp = 0L

  var filePath: String? = null

  private val saveStateListeners = ArrayList<() -> Unit>()

  fun saveFileState(filePath: String, text: String) {
    this.filePath = FileUtil.toSystemDependentName(filePath)
    val script = VimscriptParser.parse(text)
    state = script.hashCode()
    saveStateListeners.forEach { it() }
  }

  fun saveFileState(filePath: String) {
    val vimRcFile = VimRcService.findIdeaVimRc()
    val ideaVimRcText = vimRcFile?.readText() ?: ""
    saveFileState(filePath, ideaVimRcText)
  }

  fun equalTo(document: Document): Boolean {
    val fileModificationStamp = document.modificationStamp
    if (fileModificationStamp == modificationStamp) return true

    val documentString = document.charsSequence.toString()
    val script = VimscriptParser.parse(documentString)
    if (script.hashCode() != state) {
      return false
    }

    modificationStamp = fileModificationStamp
    return true
  }

  @TestOnly
  fun clear() {
    state = null
    modificationStamp = 0
    filePath = null
  }

  fun whenFileStateSaved(action: () -> Unit) {
    if (filePath != null) {
      action()
    }
    saveStateListeners.add(action)
  }

  fun unregisterStateListener(action: () -> Unit) {
    saveStateListeners.remove(action)
  }
}

class ReloadVimRc : DumbAwareAction() {
  override fun update(e: AnActionEvent) {
    val editor = e.getData(PlatformDataKeys.EDITOR) ?: run {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE) ?: run {
      e.presentation.isEnabledAndVisible = false
      return
    }

    if (virtualFile.path != VimRcFileState.filePath) {
      e.presentation.isEnabledAndVisible = false
      return
    }

    // XXX: Actually, it worth to add e.presentation.description, but it doesn't work because of some reason
    val sameDoc = VimRcFileState.equalTo(editor.document)
    e.presentation.icon = if (sameDoc) VimIcons.IDEAVIM else AllIcons.Actions.BuildLoadChanges
    e.presentation.text = if (sameDoc) MessageHelper.message("action.no.changes.text")
    else MessageHelper.message("action.reload.text")

    e.presentation.isEnabledAndVisible = true
  }

  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
    FileDocumentManager.getInstance().saveDocumentAsIs(editor.document)
    executeIdeaVimRc()
  }
}

class ReloadFloatingToolbar : AbstractFloatingToolbarProvider(ACTION_GROUP) {
  override val autoHideable: Boolean = false

  // [VERSION UPDATE] 212+
  @Suppress("OverridingDeprecatedMember")
  override val priority: Int = 0

  override fun register(component: FloatingToolbarComponent, parentDisposable: Disposable) {
    super.register(component, parentDisposable)
    val action = {
      component.scheduleShow()
    }
    VimRcFileState.whenFileStateSaved(action)
    Disposer.register(parentDisposable) {
      VimRcFileState.unregisterStateListener(action)
    }
  }
}

class ReloadFloatingToolbarActionGroup : DefaultActionGroup() {
  companion object {
    const val ACTION_GROUP = "IdeaVim.ReloadVimRc.group"
  }
}
