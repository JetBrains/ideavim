/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.setChangeMarks
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.common.argumentCaptured
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler
import com.maddyhome.idea.vim.helper.inRepeatMode
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.vimscript.model.CommandLineVimLContext
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.functions.toVimFuncref

// todo make it multicaret
private fun doOperatorAction(
  editor: VimEditor,
  context: ExecutionContext,
  textRange: TextRange,
  motionType: SelectionType,
): Boolean {
  val operatorfunc = injector.optionGroup.getOptionValue(Options.operatorfunc, OptionAccessScope.GLOBAL(editor))
  if (operatorfunc.value.isEmpty()) {
    injector.messages.showStatusBarMessage(editor, injector.messages.message("E774"))
    return false
  }

  val scriptContext = CommandLineVimLContext

  try {
    val funcref = operatorfunc.toVimFuncref(editor, context, scriptContext)

    val arg = when (motionType) {
      SelectionType.LINE_WISE -> "line"
      SelectionType.CHARACTER_WISE -> "char"
      SelectionType.BLOCK_WISE -> "block"
    }

    val saveRepeatHandler = VimRepeater.repeatHandler
    injector.markService.setChangeMarks(editor.primaryCaret(), textRange)
    KeyHandler.getInstance().reset(editor)

    val arguments = listOf(SimpleExpression(arg))
    funcref.execute(arguments, range = null, editor, context, scriptContext)

    VimRepeater.repeatHandler = saveRepeatHandler
    return true
  } catch (e: ExException) {
    injector.messages.showStatusBarMessage(editor, e.message)
    return false
  }
}

@CommandOrMotion(keys = ["g@"], modes = [Mode.NORMAL])
internal class OperatorAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override val argumentType: Argument.Type = Argument.Type.MOTION

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val argument = cmd.argument as? Argument.Motion ?: return false
    if (!editor.inRepeatMode) {
      argumentCaptured = argument
    }
    val range = getMotionRange(editor, context, argument, operatorArguments)
    if (range != null) {
      return doOperatorAction(editor, context, range, argument.getMotionType())
    }
    return false
  }

  private fun getMotionRange(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument.Motion,
    operatorArguments: OperatorArguments,
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
      operatorArguments,
    )?.normalize()?.let {
      // If we're linewise, make sure the end offset isn't just the EOL char
      if (argument.getMotionType() == SelectionType.LINE_WISE && it.endOffset < editor.fileSize()) {
        TextRange(it.startOffset, it.endOffset + 1)
      } else {
        it
      }
    }
  }
}

@CommandOrMotion(keys = ["g@"], modes = [Mode.VISUAL])
internal class VisualOperatorAction : VisualOperatorActionHandler.ForEachCaret() {
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

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
