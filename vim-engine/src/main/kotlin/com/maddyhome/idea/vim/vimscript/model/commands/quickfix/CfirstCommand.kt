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
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.commands.Command
import com.maddyhome.idea.vim.vimscript.model.commands.CommandModifier

/**
 * See `:help :cfirst`. The item number comes from the argument (`:cfirst 3`) or the count (`:3cfirst`).
 */
@ExCommand(command = "cfir[st],cr[ewind]", description = "Jump to the [count]th item of the quickfix list, or to the first one.")
data class CfirstCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags = flags(
    RangeFlag.RANGE_IS_COUNT,
    ArgumentFlag.ARGUMENT_OPTIONAL,
    Access.READ_ONLY,
  )

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val quickFix = injector.quickFixService
    val number = getCountFromArgument() ?: if (range.size() == 0) null else getCountFromRange(editor, editor.currentCaret())
    val jump = if (number == null) quickFix.jumpToFirst(editor) else quickFix.jumpToItem(editor, number)
    return reportQuickFixJump(jump)
  }
}
