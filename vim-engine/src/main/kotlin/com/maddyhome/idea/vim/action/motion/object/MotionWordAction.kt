/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.`object`

import com.intellij.vim.annotations.CommandOrMotion
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.handler.TextObjectActionHandler

@CommandOrMotion(
  keys = ["iW"],
  modes = [com.intellij.vim.annotations.Mode.VISUAL, com.intellij.vim.annotations.Mode.OP_PENDING]
)
class MotionInnerBigWordAction : TextObjectActionHandler() {

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange {
    return injector.searchHelper.findWordObject(editor, caret, count, isOuter = false, isBig = true)
  }
}

@CommandOrMotion(
  keys = ["aW"],
  modes = [com.intellij.vim.annotations.Mode.VISUAL, com.intellij.vim.annotations.Mode.OP_PENDING]
)
class MotionOuterBigWordAction : TextObjectActionHandler() {

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange {
    return injector.searchHelper.findWordObject(editor, caret, count, isOuter = true, isBig = true)
  }
}

@CommandOrMotion(
  keys = ["iw"],
  modes = [com.intellij.vim.annotations.Mode.VISUAL, com.intellij.vim.annotations.Mode.OP_PENDING]
)
class MotionInnerWordAction : TextObjectActionHandler() {

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange {
    return injector.searchHelper.findWordObject(editor, caret, count, isOuter = false, isBig = false)
  }
}

@CommandOrMotion(
  keys = ["aw"],
  modes = [com.intellij.vim.annotations.Mode.VISUAL, com.intellij.vim.annotations.Mode.OP_PENDING]
)
class MotionOuterWordAction : TextObjectActionHandler() {

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange {
    return injector.searchHelper.findWordObject(editor, caret, count, isOuter = true, isBig = false)
  }
}
