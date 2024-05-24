/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.ex

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotionOrError
import com.maddyhome.idea.vim.state.VimStateMachine.Companion.getInstance
import com.maddyhome.idea.vim.vimscript.model.CommandLineVimLContext
import java.util.*

@CommandOrMotion(keys = ["<CR>", "<C-M>", "<C-J>"], modes = [Mode.CMD_LINE])
public class ProcessExEntryAction : MotionActionHandler.AmbiguousExecution()  {
  override val flags: EnumSet<CommandFlags> = EnumSet.of(CommandFlags.FLAG_SAVE_JUMP, CommandFlags.FLAG_END_EX)
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun getMotionActionHandler(argument: Argument?): MotionActionHandler {
    return if (argument?.character == ':') ProcessExCommandEntryAction() else ProcessSearchEntryAction()
  }
}

public class ProcessSearchEntryAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun getOffset(editor: VimEditor, caret: ImmutableVimCaret, context: ExecutionContext, argument: Argument?, operatorArguments: OperatorArguments): Motion {
    if (argument == null) return Motion.Error
    return when (argument.character) {
      '/' -> injector.searchGroup.processSearchCommand(editor, argument.string, caret.offset, Direction.FORWARDS).toMotionOrError()
      '?' -> injector.searchGroup.processSearchCommand(editor, argument.string, caret.offset, Direction.BACKWARDS).toMotionOrError()
      else -> throw ExException("Unexpected search label ${argument.character}")
    }
  }
}

public class ProcessExCommandEntryAction : MotionActionHandler.SingleExecution() {
  override val motionType: MotionType = MotionType.LINE_WISE

  override fun getOffset(editor: VimEditor, context: ExecutionContext, argument: Argument?, operatorArguments: OperatorArguments): Motion {
    val panel = injector.commandLine.getActiveCommandLine()!!
    panel.deactivate(true)
    try {
      editor.mode = com.maddyhome.idea.vim.state.mode.Mode.NORMAL()
      logger.debug("processing command")
      val text = panel.text
      val shouldSkipHistory = getInstance(editor).mappingState.isExecutingMap() || injector.macro.isExecutingMacro
      injector.vimscriptExecutor.execute(text, editor, context, shouldSkipHistory, true, CommandLineVimLContext)
    } catch (e: ExException) {
      injector.messages.showStatusBarMessage(null, e.message)
      injector.messages.indicateError()
    } catch (bad: Exception) {
      logger.error("Error during command execution", bad)
      injector.messages.indicateError()
    } finally {
      injector.processGroup.isCommandProcessing = false
      injector.processGroup.modeBeforeCommandProcessing = null
    }
    // TODO support motions for commands
    return Motion.NoMotion
  }
}
