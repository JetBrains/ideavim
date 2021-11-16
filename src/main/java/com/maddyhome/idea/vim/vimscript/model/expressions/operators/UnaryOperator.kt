/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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
