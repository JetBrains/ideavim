/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.change.shift

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.DuplicableOperatorAction
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

@CommandOrMotion(keys = ["<C-T>"], modes = [Mode.INSERT])
class ShiftRightLinesAction : ChangeEditorActionHandler.ForEachCaret() {

  override val type: Command.Type = Command.Type.INSERT

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_STROKE)

  override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.changeGroup.indentLines(editor, caret, context, operatorArguments.count1, 1, operatorArguments)

    return true
  }
}

@CommandOrMotion(keys = [">"], modes = [Mode.NORMAL])
class ShiftRightMotionAction : ChangeEditorActionHandler.ForEachCaret(), DuplicableOperatorAction {
  override val type: Command.Type = Command.Type.CHANGE

  override val argumentType: Argument.Type = Argument.Type.MOTION

  override val duplicateWith: Char = '>'

  override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    argument ?: return false

    injector.changeGroup.indentMotion(editor, caret, context, argument, 1, operatorArguments)
    return true
  }
}

@CommandOrMotion(keys = [">"], modes = [Mode.VISUAL])
class ShiftRightVisualAction : VisualOperatorActionHandler.ForEachCaret() {
  override val type: Command.Type = Command.Type.CHANGE

  override fun executeAction(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    range: VimSelection,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.changeGroup.indentRange(
      editor,
      caret,
      context,
      range.toVimTextRange(false),
      cmd.count,
      1,
      operatorArguments,
    )
    return true
  }
}
