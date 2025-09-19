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
    when (expressionValue) {
      is VimDictionary -> {
        val key = index.evaluate(editor, context, vimContext).toVimString()
        return expressionValue.dictionary[key]
          ?: throw exExceptionMessage("E716", key.toOutputString())
      }

      else -> {
        val indexValue = index.evaluate(editor, context, vimContext).toVimNumber().value
        // TODO: Support negative index to retrieve item from end
        if (expressionValue is VimList && (indexValue >= expressionValue.values.size || indexValue < 0)) {
          throw exExceptionMessage("E684", indexValue)
        }
        if (indexValue < 0) {
          return VimString.EMPTY
        }

        // TODO: This looks wrong. Surely we should just return the indexed item?
        return SublistExpression(
          SimpleExpression(indexValue),
          SimpleExpression(indexValue),
          SimpleExpression(expressionValue)
        ).evaluate(editor, context, vimContext)
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
