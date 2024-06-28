/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.handler

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.helper.endOffsetInclusive
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.state.mode.selectionType

/**
 * @author Alex Plate
 *
 * Handler for TextObjects.
 *
 * This handler gets executed for each caret.
 */
abstract class TextObjectActionHandler : EditorActionHandlerBase(true) {

  final override val type: Command.Type = Command.Type.MOTION

  /**
   * Visual mode that works for this text object.
   * E.g. In visual line-wise mode, `aw` will switch to character mode.
   *   In visual character mode, `ip` will switch to line-wise mode.
   *
   * See `:h ip` for example, the last line.
   * This "visual type" is noted in lines like "When used in Visual mode it is made linewise."
   */
  abstract val visualType: TextObjectVisualType

  abstract fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange?

  /**
   * This code is called when user executes text object in visual mode. E.g. `va(a(a(`
   */
  final override fun baseExecute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (!editor.inVisualMode) return true

    val range = getRange(editor, caret, context, operatorArguments.count1, operatorArguments.count0) ?: return false

    val block = CommandFlags.FLAG_TEXT_BLOCK in cmd.flags
    val newstart = if (block || caret.offset >= caret.vimSelectionStart) range.startOffset else range.endOffsetInclusive
    val newend = if (block || caret.offset >= caret.vimSelectionStart) range.endOffsetInclusive else range.startOffset

    if (caret.vimSelectionStart == caret.offset || block) {
      caret.vimSetSelection(newstart, newstart, false)
    }

    if (visualType == TextObjectVisualType.LINE_WISE && editor.mode.selectionType != SelectionType.LINE_WISE) {
      injector.visualMotionGroup.toggleVisual(editor, 1, 0, SelectionType.LINE_WISE)
    } else if (visualType != TextObjectVisualType.LINE_WISE && editor.mode.selectionType == SelectionType.LINE_WISE) {
      injector.visualMotionGroup.toggleVisual(editor, 1, 0, SelectionType.CHARACTER_WISE)
    }

    caret.moveToOffset(newend)

    return true
  }
}
