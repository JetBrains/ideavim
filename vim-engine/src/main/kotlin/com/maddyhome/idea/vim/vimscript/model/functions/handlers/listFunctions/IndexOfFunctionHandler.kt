/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.listFunctions

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.variables.KeyVariable
import com.maddyhome.idea.vim.vimscript.model.variables.ValueVariable

@VimscriptFunction(name = "indexof")
internal class IndexOfFunctionHandler : BuiltinFunctionHandler<VimInt>(minArity = 2, maxArity = 3) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimInt {
    val o = arguments[0]
    val expr = arguments[1]
    val opts = arguments.getOrNull(2)

    when (o) {
      is VimList -> {
        if (expr !is VimString && expr !is VimFuncref) {
          throw exExceptionMessage("E1256", 2)
        }

        val funcref = expr as? VimFuncref
        val expression = (expr as? VimString)?.let {
          injector.vimscriptParser.parseExpression(it.value)
        }

        if (opts != null && opts !is VimDictionary) {
          throw exExceptionMessage("E1206", 3)
        }

        val startIndex = opts?.dictionary?.get(VimString("startidx"))?.toVimNumber()?.value ?: 0
        val start = if (startIndex < 0) startIndex + o.values.size else startIndex

        for (i in start until o.values.size) {
          try {
            val index = VimInt(i)
            val value = o.values[i]

            KeyVariable.key = index
            ValueVariable.value = value

            // TODO: Add test for funcref/expression returning wrong type
            val result = funcref?.execute(
              listOf(SimpleExpression(index), SimpleExpression(value)),
              range = null,
              editor,
              context,
              vimContext
            )?.toVimNumber()
              ?: expression?.evaluate(editor, context, vimContext)?.toVimNumber()
              ?: VimInt.ZERO

            if (result.booleanValue) {
              return index
            }
          } finally {
            KeyVariable.key = null
            ValueVariable.value = null
          }
        }
      }

      is VimBlob -> TODO()
      else -> throw exExceptionMessage("E1226", 1)
    }

    return VimInt.MINUS_ONE
  }
}
