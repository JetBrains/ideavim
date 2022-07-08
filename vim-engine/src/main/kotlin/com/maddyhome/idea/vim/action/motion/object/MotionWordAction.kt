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

package com.maddyhome.idea.vim.action.motion.`object`

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.helper.mode

class MotionInnerBigWordAction : TextObjectActionHandler() {

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
    argument: Argument?,
  ): TextRange {
    return getWordRange(editor, caret, count, isOuter = false, isBig = true)
  }
}

class MotionOuterBigWordAction : TextObjectActionHandler() {

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
    argument: Argument?,
  ): TextRange {
    return getWordRange(editor, caret, count, isOuter = true, isBig = true)
  }
}

class MotionInnerWordAction : TextObjectActionHandler() {

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
    argument: Argument?,
  ): TextRange {
    return getWordRange(editor, caret, count, isOuter = false, isBig = false)
  }
}

class MotionOuterWordAction : TextObjectActionHandler() {

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
    argument: Argument?,
  ): TextRange {
    return getWordRange(editor, caret, count, isOuter = true, isBig = false)
  }
}

fun getWordRange(
  editor: VimEditor,
  caret: VimCaret,
  count: Int,
  isOuter: Boolean,
  isBig: Boolean,
): TextRange {
  var dir = 1
  var selection = false
  if (editor.mode == VimStateMachine.Mode.VISUAL) {
    if (caret.vimSelectionStart > caret.offset.point) {
      dir = -1
    }
    if (caret.vimSelectionStart != caret.offset.point) {
      selection = true
    }
  }
  return injector.searchHelper.findWordUnderCursor(editor, caret, count, dir, isOuter, isBig, selection)
}
