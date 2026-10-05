/*
 * Copyright 2003-2023 The IdeaVim authors
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
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :substitute"
 */
@ExCommand(
  command = "~,&",
  description = "Repeat the last :substitute in [range] lines. :& uses the same search pattern, :~ uses the last used search pattern. [&] keeps the flags."
)
@ExCommand(
  command = "s[ubstitute]",
  delimitedSections = 2,
  description = "Substitute {pattern} with {string} in [range] lines, using [flags] (e.g. g for all matches, i to ignore case, c to confirm)."
)
data class SubstituteCommand(val range: Range, val argument: String, val command: String) :
  Command.SingleExecution(range, CommandModifier.NONE, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.SELF_SYNCHRONIZED)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    var result = true
    for (caret in editor.nativeCarets()) {
      val lineRange = getLineRange(editor, caret)
      if (!injector.searchGroup.processSubstituteCommand(
          editor,
          caret,
          context,
          lineRange,
          command,
          argument,
          this.vimContext
        )
      ) {
        result = false
      }
    }
    return if (result) ExecutionResult.Success else ExecutionResult.Error
  }
}
