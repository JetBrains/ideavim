/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.change.change

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.action.ex.CmdLineAction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.DuplicableOperatorAction
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.endOffsetInclusive
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.state.mode.ReturnableFromCmd
import java.util.*

@CommandOrMotion(keys = ["!"], modes = [Mode.VISUAL])
public class FilterVisualLinesAction : VimActionHandler.SingleExecution(), FilterCommand {
  override val type: Command.Type = Command.Type.CHANGE

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_MOT_LINEWISE)

  override fun execute(editor: VimEditor, context: ExecutionContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    startFilterCommand(editor, context, cmd)
    editor.exitVisualMode()
    return true
  }
}

@CommandOrMotion(keys = ["!"], modes = [Mode.NORMAL])
public class FilterMotionAction : VimActionHandler.SingleExecution(), FilterCommand, DuplicableOperatorAction {

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
    startFilterCommand(editor, context, Argument.EMPTY_COMMAND.copy(rawCount = count))
    return true
  }
}

public interface FilterCommand : CmdLineAction {
  public fun startFilterCommand(editor: VimEditor, context: ExecutionContext, cmd: Command) {
    val initText = getRange(editor, cmd) + "!"
    val currentMode = editor.mode
    check(currentMode is ReturnableFromCmd) { "Cannot enable cmd mode from $currentMode" }
    editor.mode = com.maddyhome.idea.vim.state.mode.Mode.CMD_LINE(currentMode)
    injector.commandLine.create(editor, context, ":", initText, 1)
  }
}