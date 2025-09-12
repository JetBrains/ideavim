/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.toolbar.floating.AbstractFloatingToolbarProvider
import com.intellij.openapi.editor.toolbar.floating.FloatingToolbarComponent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.maddyhome.idea.vim.api.VimrcFileState
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.icons.VimIcons
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.troubleshooting.Troubleshooter
import com.maddyhome.idea.vim.ui.ReloadFloatingToolbarActionGroup.Companion.ACTION_GROUP
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import com.maddyhome.idea.vim.vimscript.services.VimRcService.VIMRC_FILE_NAME
import com.maddyhome.idea.vim.vimscript.services.VimRcService.executeIdeaVimRc
import org.jetbrains.annotations.TestOnly
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * This file contains a "reload ~/.ideavimrc file" action functionality.
 * This is small floating action in the top right corner of the editor that appears if user edits configuration file.
 *
 * Here you can find:
 * - Simplified snapshot of config file
 * - Floating bar
 * - Action / action group
 */

internal object VimRcFileState : VimrcFileState {
  // Hash of .ideavimrc parsed to Script class
  private var state: Int? = null

  // ModificationStamp. Can be taken only from document. Doesn't play a big role, but can help speed up [equalTo]
  private var modificationStamp = 0L

  override var filePath: String? = null

  private val saveStateListeners = ArrayList<() -> Unit>()

  private val LOG = logger<VimRcFileState>()

  fun saveFileState(filePath: String, text: String) {
    this.filePath = FileUtil.toSystemDependentName(filePath)
    val script = VimscriptParser.parse(text)
    state = script.hashCode()
    saveStateListeners.forEach { it() }
  }

  override fun saveFileState(filePath: String) {
    val ideaVimRcText = Path.of(filePath).let {
      kotlin.runCatching { it.readText() }
        .onFailure { LOG.error(it) }
        .getOrNull()
    } ?: ""
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

internal class ReloadVimRc : DumbAwareAction() {
  override fun update(e: AnActionEvent) {
    val editor = e.getData(PlatformDataKeys.EDITOR) ?: run {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE) ?: run {
      e.presentation.isEnabledAndVisible = false
      return
    }

    if (VimRcFileState.filePath != null && FileUtil.toSystemDependentName(virtualFile.path) != VimRcFileState.filePath) {
      e.presentation.isEnabledAndVisible = false
      return
    } else if (VimRcFileState.filePath == null && !virtualFile.path.endsWith(VIMRC_FILE_NAME)) {
      // This if is about showing the reload icon if the IJ opens with .ideavimrc file opened.
      // At this moment VimRcFileState is not yet initialized.
      // XXX: I believe the proper solution would be to get rid of this if branch and update the action when
      //    `filePath` is set, but I wasn't able to make it work, the icon just doesn't appear. Maybe the action group
      //    or the toolbar should be updated along with the action.
      e.presentation.isEnabledAndVisible = false
      return
    }

    // XXX: Actually, it worth to add e.presentation.description, but it doesn't work because of some reason
    val sameDoc = VimRcFileState.equalTo(editor.document)
    e.presentation.icon = if (sameDoc) VimIcons.IDEAVIM else AllIcons.Actions.BuildLoadChanges
    e.presentation.text = if (sameDoc) {
      MessageHelper.message("action.ReloadVimRc.no.changes.text")
    } else {
      MessageHelper.message("action.ReloadVimRc.text")
    }

    e.presentation.isEnabledAndVisible = true
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
    injector.keyGroup.removeKeyMapping(MappingOwner.IdeaVim.InitScript)
    Troubleshooter.getInstance().removeByType("old-action-notation-in-mappings")

    // Reload the ideavimrc in the context of the current window, as though we had called `:source ~/.ideavimrc`
    executeIdeaVimRc(editor.vim)
  }
}

internal class ReloadFloatingToolbar : AbstractFloatingToolbarProvider(ACTION_GROUP) {
  override val autoHideable: Boolean = false

  override fun register(dataContext: DataContext, component: FloatingToolbarComponent, parentDisposable: Disposable) {
    super.register(dataContext, component, parentDisposable)
    val action = {
      component.scheduleShow()
    }
    VimRcFileState.whenFileStateSaved(action)
    Disposer.register(parentDisposable) {
      VimRcFileState.unregisterStateListener(action)
    }
  }
}

internal class ReloadFloatingToolbarActionGroup : DefaultActionGroup() {
  companion object {
    const val ACTION_GROUP = "IdeaVim.ReloadVimRc.group"
  }
}
