/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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
package com.maddyhome.idea.vim.action.change.insert

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.MutableVimEditor
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimVisualPosition
import com.maddyhome.idea.vim.api.injector
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
  val len = injector.engineEditorHelper.getLineLength(
    editor,
    injector.engineEditorHelper.visualLineToLogicalLine(editor, vp.line)
  )
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
