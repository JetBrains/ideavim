/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.change.change

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.DuplicableOperatorAction
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.endOffsetInclusive

public class FilterMotionAction : VimActionHandler.SingleExecution(), DuplicableOperatorAction {

  override val type: Command.Type = Command.Type.CHANGE

  override val argumentType: Argument.Type = Argument.Type.MOTION

  override val duplicateWith: Char = '!'

  override fun execute(editor: VimEditor, context: ExecutionContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    val argument = cmd.argument ?: return false
    val range = injector.motion.getMotionRange(editor, editor.primaryCaret(), context, argument, operatorArguments)
      ?: return false

    val current = editor.currentCaret().getBufferPosition()
    val start = editor.offsetToBufferPosition(range.startOffset)
    val end = editor.offsetToBufferPosition(range.endOffsetInclusive)
    if (current.line != start.line) {
      editor.primaryCaret().moveToOffset(range.startOffset)
    }

    val count = if (start.line < end.line) end.line - start.line + 1 else 1

    injector.processGroup.startFilterCommand(editor, context, Argument.EMPTY_COMMAND.copy(rawCount = count))

    return true
  }
}
