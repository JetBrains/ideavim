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
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimRedrawService
import com.maddyhome.idea.vim.api.injector

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
      injector.redrawService.redraw()
    }
  }

  /**
   * Simulates Vim's redraw when the document changes
   *
   * Only redraw if lines are added/removed.
   */
  object RedrawListener : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
      if (VimPlugin.isNotEnabled()) return
      if (event.newFragment.contains("\n") || event.oldFragment.contains("\n")) {
        injector.redrawService.redraw()
      }
    }
  }
}
