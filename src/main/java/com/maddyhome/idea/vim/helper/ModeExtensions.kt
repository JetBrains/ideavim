/*
 * Copyright 2003-2023 The IdeaVim authors
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
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.inSelectMode

/** [adjustCaretPosition] - if true, caret will be moved one char left if it's on the line end */
internal fun Editor.exitSelectMode(adjustCaretPosition: Boolean) {
  val vimEditor = this.vim
  if (!vimEditor.inSelectMode) return

  vimEditor.mode = vimEditor.mode.returnTo
  SelectionVimListenerSuppressor.lock().use {
    this.caretModel.allCarets.forEach {
      it.removeSelection()
      it.vim.vimSelectionStartClear()
      if (adjustCaretPosition) {
        val lineEnd = IjVimEditor(this).getLineEndForOffset(it.offset)
        val lineStart = IjVimEditor(this).getLineStartForOffset(it.offset)
        if (it.offset == lineEnd && it.offset != lineStart) {
          it.moveToInlayAwareOffset(it.offset - 1)
        }
      }
    }
  }
}

/** [adjustCaretPosition] - if true, caret will be moved one char left if it's on the line end */
internal fun VimEditor.exitSelectMode(adjustCaretPosition: Boolean) {
  if (!this.inSelectMode) return

  mode = mode.returnTo
  SelectionVimListenerSuppressor.lock().use {
    carets().forEach { vimCaret ->
      val caret = (vimCaret as IjVimCaret).caret
      caret.removeSelection()
      caret.vim.vimSelectionStartClear()
      if (adjustCaretPosition) {
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
  VimPlugin.getChange().processEscape(IjVimEditor(this), IjEditorExecutionContext(context))
}
