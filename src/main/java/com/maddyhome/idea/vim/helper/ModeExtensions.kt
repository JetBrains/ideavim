/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:JvmName("ModeHelper")

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.visual.VimVisualTimer
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.inSelectMode

/** [adjustCaretPosition] - if true, caret will be moved one char left if it's on the line end */
internal fun VimEditor.exitSelectMode(adjustCaretPosition: Boolean) {
  if (!this.inSelectMode) return

  // Cancel any pending visual timer mode change. When the user explicitly exits SELECT mode,
  // we don't want a delayed selection change handler to override their intent.
  VimVisualTimer.drop()
  mode = mode.returnTo
  SelectionVimListenerSuppressor.lock().use {
    carets().forEach { vimCaret ->
      val caret = (vimCaret as IjVimCaret).caret
      // NOTE: I think it should be write action, but the exception shows only an absence of the read action
      injector.application.runReadAction { caret.removeSelection() }
      caret.vim.vimSelectionStartClear()
      if (adjustCaretPosition && !isEndAllowed) {
        val lineEnd = IjVimEditor((this as IjVimEditor).editor).getLineEndForOffset(caret.offset)
        val lineStart = IjVimEditor(editor).getLineStartForOffset(caret.offset)
        if (caret.offset == lineEnd && caret.offset != lineStart) {
          caret.moveToInlayAwareOffset(caret.offset - 1)
        }
      }
    }
  }
}

internal fun Editor.exitInsertMode(context: DataContext) {
  // Cancel any pending visual timer mode change. When the user explicitly presses Escape to exit INSERT mode,
  // we don't want a delayed selection change handler to override their intent and switch back to INSERT.
  VimVisualTimer.drop()
  VimPlugin.getChange().processEscape(IjVimEditor(this), IjEditorExecutionContext(context))
}
