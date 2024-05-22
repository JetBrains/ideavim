/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.ex

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.VimPlugin
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

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
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

  override fun getOffset(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    VimPlugin.getProcess().processExEntry(editor, context)
    // TODO support motions for commands
    return Motion.NoMotion
  }
}
