/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.Script
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag

data class VariableExpression(val scope: Scope?, val name: CurlyBracesName) : LValueExpression() {
  constructor(scope: Scope?, name: String) : this(scope, CurlyBracesName(listOf(SimpleExpression(name))))

  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    return injector.variableService.getNonNullVariableValue(this, editor, context, vimContext)
  }

  fun toString(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext) =
    formatName(editor, context, vimContext)

  override fun isStronglyTyped() = false

  override fun assign(
    value: VimDataType,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
    assignmentTextForErrors: String,
  ) {
    if ((scope == Scope.SCRIPT_VARIABLE && vimContext.getFirstParentContext() !is Script) ||
      (!isInsideFunction(vimContext) && (scope == Scope.FUNCTION_VARIABLE || scope == Scope.LOCAL_VARIABLE))
    ) {
      throw exExceptionMessage("E461", formatName(editor, context, vimContext))
    }

    if (isReadOnlyVariable(editor, context, vimContext)) {
      throw exExceptionMessage("E46", formatName(editor, context, vimContext))
    }

    val leftValue = injector.variableService.getNullableVariableValue(this, editor, context, vimContext)
    if (leftValue?.isLocked == true && (leftValue.lockOwner as? VariableExpression)?.name == name) {
      throw exExceptionMessage("E741", formatName(editor, context, vimContext))
    }
    injector.variableService.storeVariable(this, value, editor, context, vimContext)
  }

  private fun formatName(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext) =
    (scope?.toString() ?: "") + name.evaluate(editor, context, vimContext).value

  private fun isInsideFunction(vimLContext: VimLContext): Boolean {
    var isInsideFunction = false
    var node = vimLContext
    while (!node.isFirstParentContext()) {
      if (node is FunctionDeclaration) {
        isInsideFunction = true
      }
      node = node.getPreviousParentContext()
    }
    return isInsideFunction
  }

  private fun isReadOnlyVariable(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): Boolean {
    if (scope == Scope.FUNCTION_VARIABLE) return true
    if (scope == null && name.evaluate(editor, context, vimContext).value == "self" && isInsideDictionaryFunction(vimContext)) {
      return true
    }
    return false
  }

  private fun isInsideDictionaryFunction(vimContext: VimLContext): Boolean {
    var node = vimContext
    while (!node.isFirstParentContext()) {
      if (node is FunctionDeclaration && node.flags.contains(FunctionFlag.DICT)) {
        return true
      }
      node = node.getPreviousParentContext()
    }
    return false
  }
}
