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
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.ReturnTo
import com.maddyhome.idea.vim.state.mode.inSelectMode
import com.maddyhome.idea.vim.state.mode.returnTo

/** [adjustCaretPosition] - if true, caret will be moved one char left if it's on the line end */
internal fun Editor.exitSelectMode(adjustCaretPosition: Boolean) {
  if (!this.vim.inSelectMode) return

  val returnTo = this.vim.mode.returnTo
  when (returnTo) {
    ReturnTo.INSERT -> {
      this.vim.mode = Mode.INSERT
    }

    ReturnTo.REPLACE -> {
      this.vim.mode = Mode.REPLACE
    }

    null -> {
      this.vim.mode = Mode.NORMAL()
    }
  }
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

  val returnTo = this.mode.returnTo
  when (returnTo) {
    ReturnTo.INSERT -> {
      this.mode = Mode.INSERT
    }

    ReturnTo.REPLACE -> {
      this.mode = Mode.REPLACE
    }

    null -> {
      this.mode = Mode.NORMAL()
    }
  }
  SelectionVimListenerSuppressor.lock().use {
    this.carets().forEach { vimCaret ->
      val caret = (vimCaret as IjVimCaret).caret
      caret.removeSelection()
      caret.vim.vimSelectionStartClear()
      if (adjustCaretPosition) {
        val lineEnd = IjVimEditor((this as IjVimEditor).editor).getLineEndForOffset(caret.offset)
        val lineStart = IjVimEditor(this.editor).getLineStartForOffset(caret.offset)
        if (caret.offset == lineEnd && caret.offset != lineStart) {
          caret.moveToInlayAwareOffset(caret.offset - 1)
        }
      }
    }
  }
}

internal fun Editor.exitInsertMode(context: DataContext, operatorArguments: OperatorArguments) {
  VimPlugin.getChange().processEscape(IjVimEditor(this), IjEditorExecutionContext(context), operatorArguments)
}
