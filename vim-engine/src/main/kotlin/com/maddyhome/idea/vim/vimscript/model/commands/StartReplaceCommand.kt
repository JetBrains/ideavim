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
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :startreplace"
 */
@ExCommand(command = "startr[eplace]")
data class StartReplaceCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_FORBIDDEN, Access.SELF_SYNCHRONIZED)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    if (modifier == CommandModifier.BANG) {
      val allowPastEnd = editor.isEndAllowed
      for (caret in editor.nativeCarets()) {
        caret.moveToOffset(injector.motion.moveCaretToRelativeLineEnd(editor, caret, 0, allowPastEnd))
      }
    }
    injector.changeGroup.changeReplace(editor, context)
    return ExecutionResult.Success
  }
}
