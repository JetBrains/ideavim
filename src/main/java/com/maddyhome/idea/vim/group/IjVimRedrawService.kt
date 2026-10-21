/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.vfs.VirtualFile
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimRedrawService
import com.maddyhome.idea.vim.api.VirtualBufferKind
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ModeChangeListener
import com.maddyhome.idea.vim.helper.CmdwinKeys
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.ui.OutputPanel

class IjVimRedrawService : VimRedrawService {
  override fun redraw() {
    // The only thing IntelliJ needs to redraw is the status line. Everything else is handled automatically.
    redrawStatusLine()
  }

  override fun redrawStatusLine() {
    injector.messages.clearStatusBarMessage()
  }

  companion object {
    /**
     * Simulate Vim's redraw when the current editor changes
     */
    fun fileEditorManagerSelectionChangedCallback(event: FileEditorManagerEvent) {
      if (isReturnFromCmdwin(event)) return
      injector.redrawService.redraw()
    }

    /**
     * Closing the cmdwin runs its command in the editor it returns to. The platform reports this asynchronously, after
     * the command's output (e.g. `:echo`) is already shown, and in two steps: from the cmdwin to no editor, and then to
     * the original editor. A redraw for either would close that output
     */
    private fun isReturnFromCmdwin(event: FileEditorManagerEvent): Boolean =
      isCmdwin(event.oldFile) || hasActiveOutputPanel(event.newEditor)

    private fun isCmdwin(file: VirtualFile?): Boolean = when (file?.getUserData(CmdwinKeys.KIND)) {
      VirtualBufferKind.Command, is VirtualBufferKind.Search -> true
      else -> false
    }

    private fun hasActiveOutputPanel(fileEditor: FileEditor?): Boolean {
      val editor = (fileEditor as? TextEditor)?.editor ?: return false
      return OutputPanel.tryGetInstance(editor)?.active == true
    }
  }

  /**
   * Simulate Vim's redraw when the document changes, or when the mode changes
   */
  internal object RedrawListener : DocumentListener, ModeChangeListener {
    override fun documentChanged(event: DocumentEvent) {
      if (VimPlugin.isNotEnabled()) return
      if (event.newFragment.contains("\n") || event.oldFragment.contains("\n")) {
        if (event.document != injector.editorGroup.getSelectedEditor()?.ij?.document) return
        injector.redrawService.redraw()
      }
    }

    override fun modeChanged(editor: VimEditor, oldMode: Mode) {
      injector.redrawService.redraw()
    }
  }
}
