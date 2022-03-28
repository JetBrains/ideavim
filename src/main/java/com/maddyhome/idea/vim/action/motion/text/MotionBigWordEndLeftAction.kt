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
package com.maddyhome.idea.vim.action.motion.text

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.Motion.AbsoluteOffset
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.SearchHelper
import com.maddyhome.idea.vim.helper.fileSize
import com.maddyhome.idea.vim.newapi.ij

sealed class WordEndAction(val direction: Direction, val bigWord: Boolean) : MotionActionHandler.ForEachCaret() {
  override fun getOffset(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return moveCaretToNextWordEnd(editor.ij, caret.ij, direction.toInt() * operatorArguments.count1, bigWord)
  }

  override val motionType: MotionType = MotionType.INCLUSIVE
}

class MotionBigWordEndLeftAction : WordEndAction(Direction.BACKWARDS, true)
class MotionBigWordEndRightAction : WordEndAction(Direction.FORWARDS, true)
class MotionWordEndLeftAction : WordEndAction(Direction.BACKWARDS, false)
class MotionWordEndRightAction : WordEndAction(Direction.FORWARDS, false)

fun moveCaretToNextWordEnd(editor: Editor, caret: Caret, count: Int, bigWord: Boolean): Motion {
  if (caret.offset == 0 && count < 0 || caret.offset >= editor.fileSize - 1 && count > 0) {
    return Motion.Error
  }

  // If we are doing this move as part of a change command (e.q. cw), we need to count the current end of
  // word if the cursor happens to be on the end of a word already. If this is a normal move, we don't count
  // the current word.
  val pos = SearchHelper.findNextWordEnd(editor, caret, count, bigWord)
  return if (pos == -1) {
    if (count < 0) {
      AbsoluteOffset(VimPlugin.getMotion().moveCaretToLineStart(editor, 0))
    } else {
      AbsoluteOffset(VimPlugin.getMotion().moveCaretToLineEnd(editor, EditorHelper.getLineCount(editor) - 1, false))
    }
  } else {
    AbsoluteOffset(pos)
  }
}
