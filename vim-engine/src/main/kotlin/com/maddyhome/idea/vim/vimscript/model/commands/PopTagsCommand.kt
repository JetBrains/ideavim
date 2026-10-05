/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :pop"
 */
@ExCommand(
  command = "po[p]",
  description = "Jump [count] entries back (older) in the tag stack, returning to where the tag jump was made."
)
data class PopTagsCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_IS_COUNT, ArgumentFlag.ARGUMENT_FORBIDDEN, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val count = getCountFromRange(editor, editor.currentCaret())
    val motion = injector.motion.moveCaretToTagForward(editor, editor.primaryCaret(), count)
    if (motion is Motion.AbsoluteOffset) {
      editor.primaryCaret().moveToOffset(motion.offset)
    }
    return ExecutionResult.Success
  }
}
