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

@CommandOrMotion(keys = ["i>", "i<lt>"], modes = [Mode.VISUAL, Mode.OP_PENDING])
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

@CommandOrMotion(keys = ["iB", "i{", "i}"], modes = [Mode.VISUAL, Mode.OP_PENDING])
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

@CommandOrMotion(keys = ["i[", "i]"], modes = [Mode.VISUAL, Mode.OP_PENDING])
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

@CommandOrMotion(keys = ["ib", "i(", "i)"], modes = [Mode.VISUAL, Mode.OP_PENDING])
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

@CommandOrMotion(keys = ["a<", "a>"], modes = [Mode.VISUAL, Mode.OP_PENDING])
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

@CommandOrMotion(keys = ["aB", "a{", "a}"], modes = [Mode.VISUAL, Mode.OP_PENDING])
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

@CommandOrMotion(keys = ["a[", "a]"], modes = [Mode.VISUAL, Mode.OP_PENDING])
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

@CommandOrMotion(keys = ["ab", "a(", "a)"], modes = [Mode.VISUAL, Mode.OP_PENDING])
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
