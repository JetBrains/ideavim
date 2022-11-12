/*
 * Copyright 2003-2022 The IdeaVim authors
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
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.newapi.IjExecutionContext
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.vim

/**
 * Pop all modes, but leave editor state. E.g. editor selection is not removed.
 */
fun Editor.popAllModes() {
  val commandState = this.vim.vimStateMachine
  while (commandState.mode != VimStateMachine.Mode.COMMAND) {
    commandState.popModes()
  }
}

@RWLockLabel.NoLockRequired
fun Editor.exitVisualMode() {
  val selectionType = SelectionType.fromSubMode(this.subMode)
  SelectionVimListenerSuppressor.lock().use {
    if (inBlockSubMode) {
      this.caretModel.removeSecondaryCarets()
    }
    if (!this.vimKeepingVisualOperatorAction) {
      this.caretModel.allCarets.forEach(Caret::removeSelection)
    }
  }
  if (this.inVisualMode) {
    this.vimLastSelectionType = selectionType
    injector.markService.setVisualSelectionMarks(this.vim)
    this.caretModel.allCarets.forEach { it.vimSelectionStartClear() }

    this.vim.vimStateMachine.popModes()
  }
}

/** [adjustCaretPosition] - if true, caret will be moved one char left if it's on the line end */
fun Editor.exitSelectMode(adjustCaretPosition: Boolean) {
  if (!this.inSelectMode) return

  this.vim.vimStateMachine.popModes()
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
fun VimEditor.exitSelectMode(adjustCaretPosition: Boolean) {
  if (!this.inSelectMode) return

  this.vimStateMachine.popModes()
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

fun Editor.exitInsertMode(context: DataContext, operatorArguments: OperatorArguments) {
  VimPlugin.getChange().processEscape(IjVimEditor(this), IjExecutionContext(context), operatorArguments)
}
