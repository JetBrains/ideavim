/*
 * Copyright 2003-2023 The IdeaVim authors
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
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

class MotionInnerBlockBackQuoteAction : TextObjectActionHandler() {

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_TEXT_BLOCK)

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
    argument: Argument?,
  ): TextRange? {
    return injector.searchHelper.findBlockQuoteInLineRange(editor, caret, '`', false)
  }
}

class MotionInnerBlockDoubleQuoteAction : TextObjectActionHandler() {

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_TEXT_BLOCK)

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
    argument: Argument?,
  ): TextRange? {
    return injector.searchHelper.findBlockQuoteInLineRange(editor, caret, '"', false)
  }
}

class MotionInnerBlockSingleQuoteAction : TextObjectActionHandler() {

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_TEXT_BLOCK)

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
    argument: Argument?,
  ): TextRange? {
    return injector.searchHelper.findBlockQuoteInLineRange(editor, caret, '\'', false)
  }
}

class MotionOuterBlockBackQuoteAction : TextObjectActionHandler() {

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_TEXT_BLOCK)

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
    argument: Argument?,
  ): TextRange? {
    return injector.searchHelper.findBlockQuoteInLineRange(editor, caret, '`', true)
  }
}

class MotionOuterBlockDoubleQuoteAction : TextObjectActionHandler() {

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_TEXT_BLOCK)

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
    argument: Argument?,
  ): TextRange? {
    return injector.searchHelper.findBlockQuoteInLineRange(editor, caret, '"', true)
  }
}

class MotionOuterBlockSingleQuoteAction : TextObjectActionHandler() {

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_TEXT_BLOCK)

  override val visualType: TextObjectVisualType = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
    argument: Argument?,
  ): TextRange? {
    return injector.searchHelper.findBlockQuoteInLineRange(editor, caret, '\'', true)
  }
}
