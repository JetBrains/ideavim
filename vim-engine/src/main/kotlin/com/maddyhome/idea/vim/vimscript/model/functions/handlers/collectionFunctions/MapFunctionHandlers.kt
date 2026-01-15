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
  override fun <T : VimDataType> getSource(expr1: T) = expr1
  override fun <T : VimDataType> getTarget(expr1: T) = expr1
}

@VimscriptFunction("mapnew")
internal class MapNewFunctionHandler : MapFunctionHandlerBase() {
  override fun <T : VimDataType> getSource(expr1: T) = expr1
  override fun <T : VimDataType> getTarget(expr1: T): T {
    @Suppress("UNCHECKED_CAST")
    return expr1.copy() as T
  }
}

internal abstract class MapFunctionHandlerBase : MapFilterFunctionHandlerBase() {
  override fun processItem(
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
}

@VimscriptFunction("filter")
internal class FilterFunctionHandler : MapFilterFunctionHandlerBase() {
  override fun <T : VimDataType> getSource(expr1: T): T {
    // Return a copy of the source expression, so we can safely modify source while iterating over the copy
    @Suppress("UNCHECKED_CAST")
    return expr1.copy() as T
  }

  override fun <T : VimDataType> getTarget(expr1: T) = expr1

  override fun processItem(
    key: VimDataType,
    value: VimDataType,
    funcref: VimFuncref?,
    expression: Expression?,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType? {
    try {
      KeyVariable.key = key
      ValueVariable.value = value

      val result = funcref?.execute(
        listOf(SimpleExpression(key), SimpleExpression(value)),
        range = null,
        editor,
        context,
        vimContext
      )
        ?: expression?.evaluate(editor, context, vimContext)
        ?: error("Funcref and expression should not both be null")

      return if (result.toVimNumber().booleanValue) value else null
    }
    finally {
      KeyVariable.key = null
      ValueVariable.value = null
    }
  }
}

internal abstract class MapFilterFunctionHandlerBase : BinaryFunctionHandler<VimDataType>() {
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
        val source = getSource(expr1)
        val target = getTarget(expr1)
        if (target.isLocked) {
          throw exExceptionMessage("E741", "${name}() argument")
        }

        var writeIndex = 0
        for ((index, value) in source.values.withIndex()) {
          val result = processItem(VimInt(index), value, funcref, expression, editor, context, vimContext)
          if (result != null) {
            if (target.values.size > index && target.values[index].isLocked) {
              throw exExceptionMessage("E741", "${name}() argument")
            }
            target.values[writeIndex++] = result
          }
          else {
            // TODO: Validate index values
            target.values.removeAt(writeIndex)
          }
        }

        return target
      }

      is VimDictionary -> {
        val source = getSource(expr1)
        val target = getTarget(expr1)
        if (target.isLocked) {
          throw exExceptionMessage("E741", "${name}() argument")
        }

        for ((k, v) in source.dictionary.entries) {
          val result = processItem(k, v, funcref, expression, editor, context, vimContext)
          if (result != null) {
            if (target.dictionary[k]?.isLocked == true) {
              throw exExceptionMessage("E741", "${name}() argument")
            }
            target.dictionary[k] = result
          }
          else {
            target.dictionary.remove(k)
          }
        }

        return target
      }

      is VimString -> {
        val string = buildString {
          // We don't need to use getSource here - we'll never modify expr1
          for ((index, ch) in expr1.value.withIndex()) {
            val result =
              processItem(VimInt(index), VimString(ch.toString()), funcref, expression, editor, context, vimContext)
            if (result != null) {
              if (result !is VimString) {
                throw exExceptionMessage("E928")
              }
              append(result.value)
            }
          }
        }

        return VimString(string)
      }

      is VimBlob -> TODO()
      else -> throw exExceptionMessage("E1250", "${name}()")
    }
  }

  protected abstract fun processItem(
    key: VimDataType,
    value: VimDataType,
    funcref: VimFuncref?,
    expression: Expression?,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ) : VimDataType?

  abstract fun <T : VimDataType> getSource(expr1: T): T
  abstract fun <T : VimDataType> getTarget(expr1: T): T
}
