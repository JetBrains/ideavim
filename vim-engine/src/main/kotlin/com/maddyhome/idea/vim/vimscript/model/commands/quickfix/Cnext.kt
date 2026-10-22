/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands.quickfix

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.QuickFixService
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.commands.Command
import com.maddyhome.idea.vim.vimscript.model.commands.CommandModifier

/**
 * See `:help :cnext`. Like Vim, a count past the end stops at the last item, and E553 is only
 * reported when the current item already is the last one.
 */
@ExCommand(command = "cn[ext]", description = "Jump to the [count]th next item in the quickfix list")
data class CnextCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags = flags(
    RangeFlag.RANGE_IS_COUNT,
    ArgumentFlag.ARGUMENT_FORBIDDEN,
    Access.READ_ONLY,
  )

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val quickFix = injector.quickFixService
    if (quickFix.size == 0) throw exExceptionMessage("E42")
    if (!quickFix.moveForward(getCountFromRange(editor, editor.currentCaret()))) throw exExceptionMessage("E553")

    val target = quickFix.jumpToCurrentItem(editor) ?: return ExecutionResult.Success
    injector.messages.showMessage(target, positionMessage(quickFix))
    return ExecutionResult.Success
  }

  /**
   * `(2 of 3): text`, without the leading whitespace of the text, like Vim's `qf_jump_print_msg`.
   */
  private fun positionMessage(quickFix: QuickFixService): String {
    val text = quickFix.currentItem?.text?.trimStart().orEmpty()
    return "(${quickFix.currentIndex + 1} of ${quickFix.size}): $text"
  }
}
