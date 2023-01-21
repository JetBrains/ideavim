/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.OptionExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser

object ExistsFunctionHandler : FunctionHandler() {

  override val name = "exists"
  override val minimumNumberOfArguments = 1
  override val maximumNumberOfArguments = 1

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val expression = argumentValues[0]
    return if (expression is SimpleExpression && expression.data is VimString) {
      val parsedValue = VimscriptParser.parseExpression((expression.data as VimString).value)
      if (parsedValue is OptionExpression) {
        if (injector.optionGroup.getOptionByNameOrAbbr(parsedValue.optionName) != null) {
          VimInt.ONE
        } else {
          VimInt.ZERO
        }
      } else {
        TODO()
      }
    } else {
      VimInt.ZERO
    }
  }
}
