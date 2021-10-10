package com.maddyhome.idea.vim.vimscript.model.expressions

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

data class SublistExpression(val from: Expression?, val to: Expression?, val expression: Expression) : Expression() {

  override fun evaluate(editor: Editor, context: DataContext, parent: Executable): VimDataType {
    val expressionValue = expression.evaluate(editor, context, parent)
    val arraySize = when (expressionValue) {
      is VimDictionary -> throw ExException("E719: Cannot slice a Dictionary")
      is VimList -> expressionValue.values.size
      else -> expressionValue.asString().length
    }
    var fromInt = Integer.parseInt(from?.evaluate(editor, context, parent)?.asString() ?: "0")
    if (fromInt < 0) {
      fromInt += arraySize
    }
    var toInt = Integer.parseInt(to?.evaluate(editor, context, parent)?.asString() ?: (arraySize - 1).toString())
    if (toInt < 0) {
      toInt += arraySize
    }
    return if (expressionValue is VimList) {
      if (fromInt > arraySize) {
        VimList(mutableListOf())
      } else if (fromInt == toInt) {
        expressionValue.values[fromInt]
      } else if (fromInt <= toInt) {
        VimList(expressionValue.values.subList(fromInt, toInt + 1))
      } else {
        VimList(mutableListOf())
      }
    } else {
      if (fromInt > arraySize) {
        VimString("")
      } else if (fromInt <= toInt) {
        if (toInt > expressionValue.asString().length - 1) {
          VimString(expressionValue.asString().substring(fromInt))
        } else {
          VimString(expressionValue.asString().substring(fromInt, toInt + 1))
        }
      } else {
        VimString("")
      }
    }
  }
}
