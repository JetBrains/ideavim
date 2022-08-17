/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim.action.change

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.common.argumentCaptured
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.newapi.ij
import java.util.*

private fun doOperatorAction(editor: VimEditor, context: ExecutionContext, textRange: TextRange, selectionType: SelectionType): Boolean {
  val operatorFunction = injector.keyGroup.operatorFunction
  if (operatorFunction == null) {
    VimPlugin.showMessage(MessageHelper.message("E774"))
    return false
  }

  val saveRepeatHandler = VimRepeater.repeatHandler
  VimPlugin.getMark().setChangeMarks(editor, textRange)
  KeyHandler.getInstance().reset(editor)
  val result = operatorFunction.apply(editor, context, selectionType)
  VimRepeater.repeatHandler = saveRepeatHandler
  return result
}

class OperatorAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override val argumentType: Argument.Type = Argument.Type.MOTION

  override fun execute(editor: VimEditor, context: ExecutionContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    val argument = cmd.argument ?: return false
    if (!editor.vimStateMachine.isDotRepeatInProgress) {
      argumentCaptured = argument
    }
    val range = getMotionRange(editor, context, argument, operatorArguments)

    if (range != null) {
      val selectionType = if (argument.motion.isLinewiseMotion()) {
        SelectionType.LINE_WISE
      } else {
        SelectionType.CHARACTER_WISE
      }
      return doOperatorAction(editor, context, range, selectionType)
    }
    return false
  }

  private fun getMotionRange(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument,
    operatorArguments: OperatorArguments
  ): TextRange? {

    // Note that we're using getMotionRange2 in order to avoid normalising the linewise range into line start
    // offsets that will be used to set the change marks. This affects things like the location of the caret in the
    // Commentary extension
    val ijEditor = editor.ij
    return MotionGroup.getMotionRange2(
      ijEditor,
      ijEditor.caretModel.primaryCaret,
      context.ij,
      argument,
      operatorArguments
    )?.normalize()?.let {

      // If we're linewise, make sure the end offset isn't just the EOL char
      if (argument.motion.isLinewiseMotion() && it.endOffset < editor.fileSize()) {
        TextRange(it.startOffset, it.endOffset + 1)
      } else {
        it
      }
    }
  }
}

class VisualOperatorAction : VisualOperatorActionHandler.ForEachCaret() {
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_EXIT_VISUAL)

  override fun executeAction(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    range: VimSelection,
    operatorArguments: OperatorArguments,
  ): Boolean {
    return doOperatorAction(editor, context, range.toVimTextRange(), range.type)
  }
}
