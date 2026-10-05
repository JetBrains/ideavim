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
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.findBlockRange
import com.maddyhome.idea.vim.handler.TextObjectActionHandler

@CommandOrMotion(
  keys = ["i>", "i<lt>"],
  modes = [Mode.VISUAL, Mode.OP_PENDING],
  description = "Inner <> block: select [count] <> blocks, excluding the angle brackets."
)
class MotionInnerBlockAngleAction : TextObjectActionHandler() {

  override val preserveSelectionAnchor: Boolean = false

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    return findBlockRange(editor, caret, '<', count, false)
  }
}

@CommandOrMotion(
  keys = ["iB", "i{", "i}"],
  modes = [Mode.VISUAL, Mode.OP_PENDING],
  description = "Inner Block: select [count] {} blocks, excluding the braces."
)
class MotionInnerBlockBraceAction : TextObjectActionHandler() {

  override val preserveSelectionAnchor: Boolean = false

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    return findBlockRange(editor, caret, '{', count, false)
  }
}

@CommandOrMotion(
  keys = ["i[", "i]"],
  modes = [Mode.VISUAL, Mode.OP_PENDING],
  description = "Inner [] block: select [count] [] blocks, excluding the brackets."
)
class MotionInnerBlockBracketAction : TextObjectActionHandler() {

  override val preserveSelectionAnchor: Boolean = false

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    return findBlockRange(editor, caret, '[', count, false)
  }
}

@CommandOrMotion(
  keys = ["ib", "i(", "i)"],
  modes = [Mode.VISUAL, Mode.OP_PENDING],
  description = "Inner block: select [count] () blocks, excluding the parentheses."
)
class MotionInnerBlockParenAction : TextObjectActionHandler() {

  override val preserveSelectionAnchor: Boolean = false

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    return findBlockRange(editor, caret, '(', count, false)
  }
}

@CommandOrMotion(
  keys = ["a<", "a>"],
  modes = [Mode.VISUAL, Mode.OP_PENDING],
  description = "A <> block: select [count] <> blocks, including the angle brackets."
)
class MotionOuterBlockAngleAction : TextObjectActionHandler() {

  override val preserveSelectionAnchor: Boolean = false

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    return findBlockRange(editor, caret, '<', count, true)
  }
}

@CommandOrMotion(
  keys = ["aB", "a{", "a}"],
  modes = [Mode.VISUAL, Mode.OP_PENDING],
  description = "A Block: select [count] {} blocks, including the braces."
)
class MotionOuterBlockBraceAction : TextObjectActionHandler() {

  override val preserveSelectionAnchor: Boolean = false

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    return findBlockRange(editor, caret, '{', count, true)
  }
}

@CommandOrMotion(
  keys = ["a[", "a]"],
  modes = [Mode.VISUAL, Mode.OP_PENDING],
  description = "A [] block: select [count] [] blocks, including the brackets."
)
class MotionOuterBlockBracketAction : TextObjectActionHandler() {

  override val preserveSelectionAnchor: Boolean = false

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    return findBlockRange(editor, caret, '[', count, true)
  }
}

@CommandOrMotion(
  keys = ["ab", "a(", "a)"],
  modes = [Mode.VISUAL, Mode.OP_PENDING],
  description = "A block: select [count] () blocks, including the parentheses."
)
class MotionOuterBlockParenAction : TextObjectActionHandler() {

  override val preserveSelectionAnchor: Boolean = false

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    return findBlockRange(editor, caret, '(', count, true)
  }
}
