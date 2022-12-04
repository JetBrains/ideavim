/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.`object`

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
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
    caret: ImmutableVimCaret,
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
    caret: ImmutableVimCaret,
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
    caret: ImmutableVimCaret,
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
    caret: ImmutableVimCaret,
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
  caret: ImmutableVimCaret,
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
