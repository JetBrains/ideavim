/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.ex

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.KeyHandler
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
import com.maddyhome.idea.vim.vimscript.model.CommandLineVimLContext
import java.util.*

@CommandOrMotion(keys = ["<CR>", "<C-M>", "<C-J>"], modes = [Mode.CMD_LINE])
class ProcessExEntryAction : MotionActionHandler.AmbiguousExecution() {
  override val flags: EnumSet<CommandFlags> = EnumSet.of(CommandFlags.FLAG_SAVE_JUMP, CommandFlags.FLAG_END_EX)
  override var motionType: MotionType = MotionType.EXCLUSIVE

  override fun getMotionActionHandler(argument: Argument?): MotionActionHandler {
    check(argument is Argument.ExString)
    if (argument.processing != null) return ExecuteDefinedInputProcessingAction()
    return if (argument.label == ':') ProcessExCommandEntryAction() else ProcessSearchEntryAction(this)
  }
}

/**
 * `c_CTRL-G` / `c_CTRL-T` - during an incremental search (`/` or `?` with 'incsearch' set), move the preview to the
 * next (`c_CTRL-G`) or previous (`c_CTRL-T`) match.
 *
 * Unlike [ProcessExEntryAction], these deliberately do not carry [CommandFlags.FLAG_END_EX]: the command line stays
 * open and the search is not executed. They simply move the incsearch "current match" highlight and caret preview.
 */
@CommandOrMotion(keys = ["<C-G>"], modes = [Mode.CMD_LINE])
class SearchAgainNextActionCommandLine : MotionActionHandler.SingleExecution() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun getOffset(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    injector.commandLine.getActiveCommandLine()?.advanceIncsearchMatch(next = true)
    return Motion.NoMotion
  }
}

@CommandOrMotion(keys = ["<C-T>"], modes = [Mode.CMD_LINE])
class SearchAgainPreviousActionCommandLine : MotionActionHandler.SingleExecution() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun getOffset(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    injector.commandLine.getActiveCommandLine()?.advanceIncsearchMatch(next = false)
    return Motion.NoMotion
  }
}

class ExecuteDefinedInputProcessingAction : MotionActionHandler.SingleExecution() {
  override val motionType: MotionType = MotionType.LINE_WISE

  override fun getOffset(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    if (argument !is Argument.ExString) return Motion.Error
    val input = argument.string
    val processing = argument.processing!!

    processing.invoke(input)
    return Motion.NoMotion
  }
}

class ProcessSearchEntryAction(private val parentAction: ProcessExEntryAction) : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType
    get() = throw RuntimeException("Parent motion type should be used, as only it is accessed by other code")

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    if (argument !is Argument.ExString) return Motion.Error
    val originalDirection = if (argument.label == '/') Direction.FORWARDS else Direction.BACKWARDS

    val effectiveCount = operatorArguments.count1 + argument.incSearchOffset
    val direction = if (effectiveCount >= 1) originalDirection else originalDirection.reverse()
    val count = if (effectiveCount >= 1) effectiveCount else 1 - effectiveCount

    val offsetAndMotion = injector.searchGroup.processSearchCommand(
      editor, argument.string, caret.offset, count, direction
    )
    // Vim doesn't treat not finding something as an error, although it might report either an error or warning message
    if (offsetAndMotion == null) return Motion.NoMotion
    parentAction.motionType = offsetAndMotion.second
    return offsetAndMotion.first.toMotionOrError()
  }
}

class ProcessExCommandEntryAction : MotionActionHandler.SingleExecution() {
  override val motionType: MotionType = MotionType.LINE_WISE

  override fun getOffset(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    if (argument !is Argument.ExString) return Motion.Error

    try {
      // Exit Command-line mode and return to the previous mode before executing the command (this is set to Normal in
      // startExEntry). Remember from startExEntry that we might still have selection and/or multiple carets, even
      // though we're in Normal. This will be handled by Command.execute once we know if we should be clearing the
      // selection.
      editor.mode = editor.mode.returnTo

      logger.debug("processing command")

      val text = argument.string
      val keyState = KeyHandler.getInstance().keyHandlerState
      val shouldSkipHistory = keyState.mappingState.isExecutingMap() || injector.macro.isExecutingMacro
      injector.vimscriptExecutor.execute(text, editor, context, shouldSkipHistory, true, CommandLineVimLContext)
    } catch (e: ExException) {
      injector.messages.showErrorMessage(editor, e.message)
    } catch (bad: Exception) {
      logger.error("Error during command execution", bad)
      injector.messages.indicateError()
    }
    // TODO support motions for commands
    return Motion.NoMotion
  }
}
