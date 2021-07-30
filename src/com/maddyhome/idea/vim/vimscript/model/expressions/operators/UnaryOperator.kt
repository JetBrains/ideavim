package com.maddyhome.idea.vim.vimscript.model.expressions.operators

import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.unary.MinusOperatorHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.unary.NotOperatorHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.unary.PlusOperatorHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.unary.UnaryOperatorHandler

enum class UnaryOperator(val value: String, val handler: UnaryOperatorHandler) {
  NOT("!", NotOperatorHandler),
  PLUS("+", PlusOperatorHandler),
  MINUS("-", MinusOperatorHandler);

  companion object {
    fun getByValue(value: String): UnaryOperator {
      return values().first { it.value == value }
    }
  }
}
