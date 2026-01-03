/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.collectionFunctions

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.functions.BinaryFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.variables.KeyVariable
import com.maddyhome.idea.vim.vimscript.model.variables.ValueVariable

@VimscriptFunction("foreach")
internal class ForEachFunctionHandler : BinaryFunctionHandler<VimDataType>() {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val expr1 = arguments[0]
    val iterable = when (expr1) {
      is VimList -> expr1.values.mapIndexed { index, item -> VimInt(index) to item }
      is VimDictionary -> expr1.dictionary.entries.map { it.key to it.value }
      is VimString -> expr1.value.mapIndexed { index, item -> VimInt(index) to VimString(item.toString()) }
      else -> throw exExceptionMessage("E1250", "foreach()")
    }

    val expr2 = arguments[1]  // String or Funcref
    for ((key, value) in iterable) {
      if (evaluateForItem(expr2, key, value, editor, context, vimContext) != ExecutionResult.Success) {
        break
      }
    }

    return expr1
  }

  private fun evaluateForItem(
    expr2: VimDataType,
    key: VimDataType,
    value: VimDataType,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext
  ): ExecutionResult {
    KeyVariable.key = key
    ValueVariable.value = value

    try {
      when (expr2) {
        is VimFuncref -> {
          // TODO: We don't have a way of returning error when invoking a function
          expr2.execute(listOf(SimpleExpression(key), SimpleExpression(value)), null, editor, context, vimContext)
          return if (injector.messages.isError()) ExecutionResult.Error else ExecutionResult.Success
        }

        is VimString -> {
          // Note that expr2 is a command, not an expression! We can't use parseCommand, because that doesn't handle
          // multiple commands separated by bar `|`.
          // If an error occurs, we're supposed to stop evaluating. However, execute does not give us any control over
          // the reported message
          return injector.vimscriptExecutor.execute(
            expr2.value,
            editor,
            context,
            skipHistory = true,
            indicateErrors = true,
            vimContext
          )
        }

        else -> { return ExecutionResult.Success }
      }
    }
    finally {
      KeyVariable.key = null
      ValueVariable.value = null
    }
  }
}
