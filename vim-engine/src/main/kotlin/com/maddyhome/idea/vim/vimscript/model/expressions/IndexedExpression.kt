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
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag

// expression[index]
// expression.index (entry in a dictionary)
data class IndexedExpression(val index: Expression, val expression: Expression) : LValueExpression() {

  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    val expressionValue = expression.evaluate(editor, context, vimContext)
    val indexValue = index.evaluate(editor, context, vimContext)

    // Vim seems to validate the index by converting it to a string, allowing for Float, but it will use Number to
    // index. This gets us the same error messages as Vim
    val stringIndex = if (indexValue is VimFloat) VimString(indexValue.toOutputString()) else indexValue.toVimString()

    when (expressionValue) {
      is VimDictionary -> {
        return expressionValue.dictionary[stringIndex]
          ?: throw exExceptionMessage("E716", indexValue.toOutputString())
      }

      is VimList -> {
        val idx = indexValue.toVimNumber().value
        val i = if (idx < 0) (idx + expressionValue.values.size) else idx
        if (i < 0 || i >= expressionValue.values.size) {
          throw exExceptionMessage("E684", idx)
        }
        return expressionValue.values[i]
      }

      else -> {
        // Try to convert the expression to String, then index it
        val text = expressionValue.toVimString().value
        val idx = index.evaluate(editor, context, vimContext).toVimNumber().value
        if (idx < 0 || idx > text.length) {
          return VimString.EMPTY
        }
        return VimString(text[idx].toString())
      }
    }
  }

  override fun isStronglyTyped() = false

  override fun assign(
    value: VimDataType,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
    assignmentTextForErrors: String,
  ) {
    when (val containerValue = expression.evaluate(editor, context, vimContext)) {
      is VimDictionary -> assignToDictionaryItem(containerValue, value, editor, context, vimContext, assignmentTextForErrors)
      is VimList -> assignToListItem(containerValue, value, editor, context, vimContext, assignmentTextForErrors)
      is VimBlob -> TODO()
      else -> {
        throw exExceptionMessage("E689", getTypeName(containerValue), assignmentTextForErrors)
      }
    }
  }

  private fun assignToDictionaryItem(
    dict: VimDictionary,
    value: VimDataType,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
    assignmentTextForErrors: String,
  ) {
    val k = index.evaluate(editor, context, vimContext)
    val key = if (k is VimFloat) VimString(k.toOutputString()) else k.toVimString()
    if (key in dict.dictionary) {
      if (dict.dictionary[key]?.isLocked == true) {
        // Cannot modify this item
        throw exExceptionMessage("E741", assignmentTextForErrors)
      }
    }
    else if (dict.isLocked) {
      // Cannot add a new item
      throw exExceptionMessage("E741", assignmentTextForErrors)
    }

    var newValue = value
    if (value is VimFuncref
      && !value.isSelfFixed
      && value.handler is DefinedFunctionHandler
      && value.handler.function.flags.contains(FunctionFlag.DICT)
    ) {
      newValue = value.copy()
      newValue.dictionary = dict
    }
    dict.dictionary[key] = newValue
  }

  private fun assignToListItem(
    list: VimList,
    value: VimDataType,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
    assignmentTextForErrors: String
  ) {
    // TODO: Negative index, float index, string index, blah blah blah
    val index = index.evaluate(editor, context, vimContext).toVimNumber().value
    if (index > list.values.size - 1) {
      throw exExceptionMessage("E684", index)
    }
    if (list.values[index].isLocked) {
      throw exExceptionMessage("E741", assignmentTextForErrors)
    }
    list.values[index] = value
  }

  private fun getTypeName(dataType: VimDataType): String {
    return when (dataType) {
      is VimBlob -> "blob"
      is VimDictionary -> "dict"
      is VimFloat -> "float"
      is VimFuncref -> "funcref"
      is VimInt -> "number"
      is VimList -> "list"
      is VimString -> "string"
      else -> "unknown"
    }
  }
}
