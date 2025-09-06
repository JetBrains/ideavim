/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.variousFunctions

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt
import com.maddyhome.idea.vim.vimscript.model.expressions.OptionExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.VariableExpression
import com.maddyhome.idea.vim.vimscript.model.functions.UnaryFunctionHandler

@VimscriptFunction(name = "exists")
internal class ExistsFunctionHandler : UnaryFunctionHandler<VimInt>() {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimInt {
    val expressionValue = arguments.getString(0)
    val parsedExpression = injector.vimscriptParser.parseExpression(expressionValue.value)
    val result = when (parsedExpression) {
      is OptionExpression -> {
        injector.optionGroup.getOption(parsedExpression.optionName) != null
      }

      is VariableExpression -> {
        injector.variableService.getNullableVariableValue(parsedExpression, editor, context, vimContext) != null
      }

      else -> throw ExException("exists function is not fully implemented")
    }
    return result.asVimInt()
  }
}
