/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.functions.BinaryFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.variables.KeyVariable
import com.maddyhome.idea.vim.vimscript.model.variables.ValueVariable

@VimscriptFunction("map")
internal class MapFunctionHandler : MapFunctionHandlerBase() {
  override fun getTargetList(expr1: VimList) = expr1
  override fun getTargetDictionary(expr1: VimDictionary) = expr1
}

@VimscriptFunction("mapnew")
internal class MapNewFunctionHandler : MapFunctionHandlerBase() {
  override fun getTargetList(expr1: VimList): VimList = expr1.copy()
  override fun getTargetDictionary(expr1: VimDictionary): VimDictionary = expr1.copy()
}

internal abstract class MapFunctionHandlerBase : BinaryFunctionHandler<VimDataType>() {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext
  ): VimDataType {
    val expr1 = arguments[0]
    val expr2 = arguments[1]

    if (expr1 !is VimList && expr1 !is VimDictionary && expr1 !is VimString && expr1 !is VimBlob) {
      throw exExceptionMessage("E1250", "${name}()")
    }

    val funcref = expr2 as? VimFuncref
    val expression = if (funcref != null) {
      null
    } else if (expr2 is VimFloat) {
      // Vim doesn't normally convert Float to String
      SimpleExpression(expr2)
    } else {
      val string = expr2.toVimString()
      if (string.value.isEmpty()) {
        throw exExceptionMessage("E15", string.value)
      }
      injector.vimscriptParser.parseExpression(string.value)
    }
    if (funcref == null && expression == null) {
      throw exExceptionMessage("E1250", "${name}()")
    }

    when (expr1) {
      is VimList -> {
        val target = getTargetList(expr1)
        if (target.isLocked) {
          throw exExceptionMessage("E741", "${name}() argument")
        }

        for ((index, value) in expr1.values.withIndex()) {
          val result = mapItem(VimInt(index), value, funcref, expression, editor, context, vimContext)
          if (target.values.size > index && target.values[index].isLocked) {
            throw exExceptionMessage("E741", "${name}() argument")
          }
          target.values[index] = result
        }

        return target
      }

      is VimDictionary -> {
        val target = getTargetDictionary(expr1)
        if (target.isLocked) {
          throw exExceptionMessage("E741", "${name}() argument")
        }

        for ((k, v) in expr1.dictionary.entries) {
          val result = mapItem(k, v, funcref, expression, editor, context, vimContext)
          if (target.dictionary[k]?.isLocked == true) {
            throw exExceptionMessage("E741", "${name}() argument")
          }
          target.dictionary[k] = result
        }

        return target
      }

      is VimString -> {
        val string = buildString {
          for ((index, ch) in expr1.value.withIndex()) {
            val result =
              mapItem(VimInt(index), VimString(ch.toString()), funcref, expression, editor, context, vimContext)
            if (result !is VimString) {
              throw exExceptionMessage("E928")
            }
            append(result.value)
          }
        }

        return VimString(string)
      }

      is VimBlob -> TODO()
      else -> throw exExceptionMessage("E1250", "${name}()")
    }
  }

  private fun mapItem(
    key: VimDataType,
    value: VimDataType,
    funcref: VimFuncref?,
    expression: Expression?,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    try {
      KeyVariable.key = key
      ValueVariable.value = value

      return funcref?.execute(
        listOf(SimpleExpression(key), SimpleExpression(value)),
        range = null,
        editor,
        context,
        vimContext
      )
        ?: expression?.evaluate(editor, context, vimContext)
        ?: error("Funcref and expression should not both be null")
    }
    finally {
      KeyVariable.key = null
      ValueVariable.value = null
    }
  }

  abstract fun getTargetList(expr1: VimList): VimList
  abstract fun getTargetDictionary(expr1: VimDictionary): VimDictionary
}
