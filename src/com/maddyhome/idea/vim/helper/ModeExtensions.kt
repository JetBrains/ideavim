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
    commandState.popState()
  }
}

fun Editor.hardResetAllModes() {
  val commandState = this.commandState
  while (!this.inNormalMode) {
    val statesBefore = commandState.toSimpleString()
    when (mode) {
      CommandState.Mode.VISUAL -> exitVisualMode()
      CommandState.Mode.SELECT -> exitSelectMode(true)
      CommandState.Mode.INSERT -> exitInsertModeHardReset()
      else -> commandState.popState()
    }
    if (statesBefore == commandState.toSimpleString()) {
      // This is just a protection against infinite loop
      commandState.popState()
    }
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

    this.commandState.popState()
  }
}

/** [adjustCaretPosition] - if true, caret will be moved one char left if it's on the line end */
fun Editor.exitSelectMode(adjustCaretPosition: Boolean) {
  if (!this.inSelectMode) return

  this.commandState.popState()
  SelectionVimListenerSuppressor.lock().use {
    this.caretModel.allCarets.forEach {
      it.removeSelection()
      it.vimSelectionStartClear()
      if (adjustCaretPosition) {
        val lineEnd = EditorHelper.getLineEndForOffset(this, it.offset)
        val lineStart = EditorHelper.getLineStartForOffset(this, it.offset)
        if (it.offset == lineEnd && it.offset != lineStart) {
          it.moveToOffset(it.offset - 1)
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
