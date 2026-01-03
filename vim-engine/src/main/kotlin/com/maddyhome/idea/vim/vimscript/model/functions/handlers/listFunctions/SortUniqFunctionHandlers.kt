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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler
import java.text.Collator

@VimscriptFunction(name = "sort")
internal class SortFunctionHandler : SortUniqFunctionHandlerBase() {
  override fun processList(list: VimList, comparator: Comparator<in VimDataType>) {
    list.values.sortWith(comparator)
  }
}

@VimscriptFunction(name = "uniq")
internal class UniqFunctionHandler : SortUniqFunctionHandlerBase() {
  override fun processList(list: VimList, comparator: Comparator<in VimDataType>) {
    var last: VimDataType? = null
    val iterator = list.values.iterator()
    while (iterator.hasNext()) {
      val next = iterator.next()
      if (last != null && comparator.compare(last, next) == 0) {
        iterator.remove()
      }
      last = next
    }
  }
}

internal abstract class SortUniqFunctionHandlerBase : BuiltinFunctionHandler<VimList>(minArity = 1, maxArity = 3) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimList {
    val list = arguments[0]
    if (list !is VimList) {
      throw exExceptionMessage("E686", "${name}()")
    }

    if (list.isLocked) {
      throw exExceptionMessage("E741", "${name}() argument")
    }

    val how = arguments.getOrNull(1)?.let { it as? VimFuncref ?: it.toVimString() }

    val dict = arguments.getOrNull(2)
    if (dict != null && dict !is VimDictionary) {
      throw exExceptionMessage("E1206", 3)
    }

    val comparator = when (how) {
      is VimFuncref -> {
        val func = if (dict != null) how.apply(dict) else how
        { a: VimDataType, b: VimDataType ->
          val args = listOf(SimpleExpression(a), SimpleExpression(b))
          val result = func.execute(args, range = null, editor, context, vimContext)
          result.toVimNumber().value
        }
      }

      is VimString -> {
        when (how.value) {
          "i", "1" -> getTypeAndStringValueComparator(String.CASE_INSENSITIVE_ORDER)
          "l" -> getTypeAndStringValueComparator(Collator.getInstance())

          // Allows Float and Number, everything else is treated as 0
          "n" -> { a: VimDataType, b: VimDataType ->
            val a1 = a.toNumericalValueOrZero()
            val b1 = b.toNumericalValueOrZero()
            a1.compareTo(b1)
          }

          // Allows Number, String is parsed. Float is not allowed
          "N" -> { a: VimDataType, b: VimDataType ->
            val a1 = a.toVimNumber().value
            val b1 = b.toVimNumber().value
            a1.compareTo(b1)
          }

          // Allows Float and Number only
          "f" -> { a: VimDataType, b: VimDataType ->
            val a1 = a.toNumericalValue()
            val b1 = b.toNumericalValue()
            a1.compareTo(b1)
          }

          "", "0" -> getTypeAndStringValueComparator(Comparator.naturalOrder())

          else -> {
            val handler = injector.functionService.getFunctionHandlerOrNull(Scope.GLOBAL_VARIABLE, how.value, vimContext)
              ?: throw exExceptionMessage("E117", how.value)
            val funcref = VimFuncref(handler, VimList(mutableListOf()), dict, VimFuncref.Type.FUNCTION);
            { a: VimDataType, b: VimDataType ->
              funcref.execute(
                listOf(SimpleExpression(a), SimpleExpression(b)),
                range = null,
                editor,
                context,
                vimContext
              ).toVimNumber().value
            }
          }
        }
      }

      null -> getTypeAndStringValueComparator(Comparator.naturalOrder())
      else -> throw exExceptionMessage("E474")
    }

    processList(list, comparator)
    return list
  }

  private fun getTypeAndStringValueComparator(comparator: Comparator<in String>) = { a: VimDataType, b: VimDataType ->
      val aOrder = a.sortOrder()
      val bOrder = b.sortOrder()
      when {
        aOrder == bOrder -> comparator.compare(a.toOutputString(), b.toOutputString())
        aOrder < bOrder -> -1
        else -> 1
      }
    }

  private fun VimDataType.sortOrder() = when (this) {
    is VimString -> 1
    is VimInt, is VimFloat -> 2
    is VimList -> 3
    is VimFuncref -> 4
    is VimDictionary -> 5
    is VimBlob -> 6
    else -> 7
  }

  private fun VimDataType.toNumericalValue() = when (this) {
    is VimInt -> value.toDouble()
    else -> toVimFloat().value
  }

  private fun VimDataType.toNumericalValueOrZero() = when (this) {
    is VimFloat -> toVimFloat().value
    is VimInt -> toVimNumber().value.toDouble()
    else -> 0.0
  }

  protected abstract fun processList(list: VimList, comparator: Comparator<in VimDataType>)
}
