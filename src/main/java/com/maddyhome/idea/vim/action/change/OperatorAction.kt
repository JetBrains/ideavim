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
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
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
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.inRepeatMode
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.vimscript.model.CommandLineVimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.expressions.FunctionCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression

// todo make it multicaret
private fun doOperatorAction(editor: VimEditor, context: ExecutionContext, textRange: TextRange, selectionType: SelectionType): Boolean {
  val func = injector.globalOptions().operatorfunc
  if (func.isEmpty()) {
    VimPlugin.showMessage(MessageHelper.message("E774"))
    return false
  }

  val scriptContext = CommandLineVimLContext

  // The option value is either a function name, which should have a handler, or it might be a lambda expression, or a
  // `function` or `funcref` call expression, all of which will return a funcref (with a handler)
  var handler = injector.functionService.getFunctionHandlerOrNull(null, func, scriptContext)
  if (handler == null) {
    val expression = injector.vimscriptParser.parseExpression(func)
    if (expression != null) {
      try {
        val value = expression.evaluate(editor, context, scriptContext)
        if (value is VimFuncref) {
          handler = value.handler
        }
      } catch (ex: ExException) {
        // Get the argument for function('...') or funcref('...') for the error message
        val functionName = if (expression is FunctionCallExpression && expression.arguments.size > 0) {
          expression.arguments[0].evaluate(editor, context, scriptContext).toString()
        }
        else {
          func
        }

        VimPlugin.showMessage("E117: Unknown function: $functionName")
        return false
      }
    }
  }

  if (handler == null) {
    VimPlugin.showMessage("E117: Unknown function: $func")
    return false
  }

  val arg = when (selectionType) {
    SelectionType.LINE_WISE -> "line"
    SelectionType.CHARACTER_WISE -> "char"
    SelectionType.BLOCK_WISE -> "block"
  }

  val saveRepeatHandler = VimRepeater.repeatHandler
  injector.markService.setChangeMarks(editor.primaryCaret(), textRange)
  KeyHandler.getInstance().reset(editor)

  val arguments = listOf(SimpleExpression(arg))
  handler.executeFunction(arguments, editor, context, scriptContext)

  VimRepeater.repeatHandler = saveRepeatHandler
  return true
}

@CommandOrMotion(keys = ["g@"], modes = [Mode.NORMAL])
internal class OperatorAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override val argumentType: Argument.Type = Argument.Type.MOTION

  override fun execute(editor: VimEditor, context: ExecutionContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    val argument = cmd.argument ?: return false
    if (!editor.inRepeatMode) {
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
      if (argument.motion.isLinewiseMotion() && it.endOffset < editor.fileSize()) {
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
