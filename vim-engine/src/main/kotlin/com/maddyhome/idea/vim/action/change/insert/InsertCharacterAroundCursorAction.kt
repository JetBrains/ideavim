/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change.insert

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.MutableVimEditor
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimVisualPosition
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.lineLength
import com.maddyhome.idea.vim.api.visualLineToBufferLine
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler

class InsertCharacterAboveCursorAction : ChangeEditorActionHandler.ForEachCaret() {
  override val type: Command.Type = Command.Type.INSERT

  override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    return if (editor.isOneLineMode()) {
      false
    } else insertCharacterAroundCursor(editor, caret, -1)
  }
}

class InsertCharacterBelowCursorAction : ChangeEditorActionHandler.ForEachCaret() {
  override val type: Command.Type = Command.Type.INSERT

  override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    return if (editor.isOneLineMode()) {
      false
    } else insertCharacterAroundCursor(editor, caret, 1)
  }
}

/**
 * Inserts the character above/below the cursor at the cursor location
 *
 * @param editor The editor to insert into
 * @param caret  The caret to insert after
 * @param dir    1 for getting from line below cursor, -1 for getting from line above cursor
 * @return true if able to get the character and insert it, false if not
 */
private fun insertCharacterAroundCursor(editor: VimEditor, caret: VimCaret, dir: Int): Boolean {
  var res = false
  var vp = caret.getVisualPosition()
  vp = VimVisualPosition(vp.line + dir, vp.column, false)
  val len = editor.lineLength(editor.visualLineToBufferLine(vp.line))
  if (vp.column < len) {
    val offset = editor.visualPositionToOffset(VimVisualPosition(vp.line, vp.column, false)).point
    val charsSequence = editor.text()
    if (offset < charsSequence.length) {
      val ch = charsSequence[offset]
      (editor as MutableVimEditor).insertText(caret.offset, ch.toString())
      injector.motion.moveCaret(
        editor, caret, injector.motion.getOffsetOfHorizontalMotion(editor, caret, 1, true)
      )
      res = true
    }
  }
  return res
}
