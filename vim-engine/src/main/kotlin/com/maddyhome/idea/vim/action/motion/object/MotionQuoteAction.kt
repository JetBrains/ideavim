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

@CommandOrMotion(
  keys = ["i`"],
  modes = [Mode.VISUAL, Mode.OP_PENDING],
  description = "Inner backtick string: select the text between backticks, excluding the quotes."
)
class MotionInnerBlockBackQuoteAction : TextObjectActionHandler() {

  override val preserveSelectionAnchor: Boolean = false

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    return injector.searchHelper.findBlockQuoteInLineRange(editor, caret, '`', false, includeQuotes = count >= 2)
  }
}

@CommandOrMotion(
  keys = ["i\""],
  modes = [Mode.VISUAL, Mode.OP_PENDING],
  description = "Inner double-quoted string: select the text between double quotes, excluding the quotes."
)
class MotionInnerBlockDoubleQuoteAction : TextObjectActionHandler() {

  override val preserveSelectionAnchor: Boolean = false

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    return injector.searchHelper.findBlockQuoteInLineRange(editor, caret, '"', false, includeQuotes = count >= 2)
  }
}

@CommandOrMotion(
  keys = ["i'"],
  modes = [Mode.VISUAL, Mode.OP_PENDING],
  description = "Inner single-quoted string: select the text between single quotes, excluding the quotes."
)
class MotionInnerBlockSingleQuoteAction : TextObjectActionHandler() {

  override val preserveSelectionAnchor: Boolean = false

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    return injector.searchHelper.findBlockQuoteInLineRange(editor, caret, '\'', false, includeQuotes = count >= 2)
  }
}

@CommandOrMotion(
  keys = ["a`"],
  modes = [Mode.VISUAL, Mode.OP_PENDING],
  description = "A backtick string: select the text between backticks, including the quotes and trailing white space."
)
class MotionOuterBlockBackQuoteAction : TextObjectActionHandler() {

  override val preserveSelectionAnchor: Boolean = false

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    return injector.searchHelper.findBlockQuoteInLineRange(editor, caret, '`', true)
  }
}

@CommandOrMotion(
  keys = ["a\""],
  modes = [Mode.VISUAL, Mode.OP_PENDING],
  description = "A double-quoted string: select the quoted text, including the quotes and trailing white space."
)
class MotionOuterBlockDoubleQuoteAction : TextObjectActionHandler() {

  override val preserveSelectionAnchor: Boolean = false

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    return injector.searchHelper.findBlockQuoteInLineRange(editor, caret, '"', true)
  }
}

@CommandOrMotion(
  keys = ["a'"],
  modes = [Mode.VISUAL, Mode.OP_PENDING],
  description = "A single-quoted string: select the quoted text, including the quotes and trailing white space."
)
class MotionOuterBlockSingleQuoteAction : TextObjectActionHandler() {

  override val preserveSelectionAnchor: Boolean = false

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    return injector.searchHelper.findBlockQuoteInLineRange(editor, caret, '\'', true)
  }
}
