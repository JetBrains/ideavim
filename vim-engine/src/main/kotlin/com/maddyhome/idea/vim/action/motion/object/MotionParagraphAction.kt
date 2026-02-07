/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.`object`

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.handler.TextObjectActionHandler

@CommandOrMotion(keys = ["ap"], modes = [Mode.VISUAL, Mode.OP_PENDING])
class MotionOuterParagraphAction : TextObjectActionHandler() {

  override val preserveSelectionAnchor: Boolean = false

  override val visualType: TextObjectVisualType = TextObjectVisualType.LINE_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    return injector.searchHelper.findParagraphRange(editor, caret, count, true)
  }
}

@CommandOrMotion(keys = ["ip"], modes = [Mode.VISUAL, Mode.OP_PENDING])
class MotionInnerParagraphAction : TextObjectActionHandler() {

  override val preserveSelectionAnchor: Boolean = false

  override val visualType: TextObjectVisualType = TextObjectVisualType.LINE_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    return injector.searchHelper.findParagraphRange(editor, caret, count, false)
  }
}
