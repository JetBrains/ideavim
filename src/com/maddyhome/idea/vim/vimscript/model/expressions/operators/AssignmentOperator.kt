package com.maddyhome.idea.vim.vimscript.model.expressions.operators

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.AdditionHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.ConcatenationHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.DivisionHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.ModulusHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.MultiplicationHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.SubtractionHandler

enum class AssignmentOperator(val value: String) {
  ASSIGNMENT("="),
  ADDITION("+="),
  SUBTRACTION("-="),
  MULTIPLICATION("*="),
  DIVISION("/="),
  MODULUS("%="),
  CONCATENATION(".=");

  companion object {
    fun getByValue(value: String): AssignmentOperator {
      return values().first { it.value == value }
    }
  }

  fun getNewValue(
    variable: Expression,
    value: Expression,
    editor: Editor?,
    context: DataContext?,
    vimContext: VimContext,
  ): VimDataType {
    val valueValue = value.evaluate(editor, context, vimContext)
    return when (this) {
      ASSIGNMENT -> valueValue
      ADDITION -> AdditionHandler.performOperation(variable.evaluate(editor, context, vimContext), valueValue)
      SUBTRACTION -> SubtractionHandler.performOperation(variable.evaluate(editor, context, vimContext), valueValue)
      MULTIPLICATION -> MultiplicationHandler.performOperation(
        variable.evaluate(editor, context, vimContext),
        valueValue
      )
      DIVISION -> DivisionHandler.performOperation(variable.evaluate(editor, context, vimContext), valueValue)
      MODULUS -> ModulusHandler.performOperation(variable.evaluate(editor, context, vimContext), valueValue)
      CONCATENATION -> ConcatenationHandler.performOperation(variable.evaluate(editor, context, vimContext), valueValue)
    }
  }
}
