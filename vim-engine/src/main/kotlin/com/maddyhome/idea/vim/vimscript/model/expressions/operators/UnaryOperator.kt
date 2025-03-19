/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators

import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.unary.MinusOperatorHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.unary.NotOperatorHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.unary.PlusOperatorHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.unary.UnaryOperatorHandler

enum class UnaryOperator(val value: String, internal val handler: UnaryOperatorHandler) {
  NOT("!", NotOperatorHandler),
  PLUS("+", PlusOperatorHandler),
  MINUS("-", MinusOperatorHandler),
  ;

  companion object {
    fun getByValue(value: String): UnaryOperator {
      return entries.first { it.value == value }
    }
  }
}
