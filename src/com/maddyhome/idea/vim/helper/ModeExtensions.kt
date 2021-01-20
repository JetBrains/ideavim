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

@file:JvmName("ModeHelper")

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.visual.updateCaretState
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor

/**
 * Pop all modes, but leave editor state. E.g. editor selection is not removed.
 */
fun Editor.popAllModes() {
  val commandState = this.commandState
  while (commandState.mode != CommandState.Mode.COMMAND) {
    commandState.popModes()
  }
}

@RWLockLabel.NoLockRequired
fun Editor.exitVisualMode() {
  val selectionType = SelectionType.fromSubMode(this.subMode)
  SelectionVimListenerSuppressor.lock().use {
    if (inBlockSubMode) {
      this.caretModel.allCarets.forEach { it.visualAttributes = this.caretModel.primaryCaret.visualAttributes }
      this.caretModel.removeSecondaryCarets()
    }
    if (!this.vimKeepingVisualOperatorAction) {
      this.caretModel.allCarets.forEach(Caret::removeSelection)
    }
  }
  if (this.inVisualMode) {
    this.vimLastSelectionType = selectionType
    val primaryCaret = this.caretModel.primaryCaret
    val vimSelectionStart = primaryCaret.vimSelectionStart
    VimPlugin.getMark().setVisualSelectionMarks(this, TextRange(vimSelectionStart, primaryCaret.offset))
    this.caretModel.allCarets.forEach { it.vimSelectionStartClear() }

    this.subMode = CommandState.SubMode.NONE

    this.commandState.popModes()
  }
}

/** [adjustCaretPosition] - if true, caret will be moved one char left if it's on the line end */
fun Editor.exitSelectMode(adjustCaretPosition: Boolean) {
  if (!this.inSelectMode) return

  this.commandState.popModes()
  SelectionVimListenerSuppressor.lock().use {
    this.caretModel.allCarets.forEach {
      it.removeSelection()
      it.vimSelectionStartClear()
      if (adjustCaretPosition) {
        val lineEnd = EditorHelper.getLineEndForOffset(this, it.offset)
        val lineStart = EditorHelper.getLineStartForOffset(this, it.offset)
        if (it.offset == lineEnd && it.offset != lineStart) {
          it.moveToInlayAwareOffset(it.offset - 1)
        }
      }
    }
  }
  updateCaretState(this)
}

fun Editor.exitInsertMode(context: DataContext) {
  VimPlugin.getChange().processEscape(this, context)
}

fun Editor.exitInsertModeHardReset() {
  VimPlugin.getChange().processEscape(this, null)
}
