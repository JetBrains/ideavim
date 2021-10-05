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

package com.maddyhome.idea.vim.vimscript.model.datatypes

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler
import com.maddyhome.idea.vim.vimscript.services.FunctionStorage

data class VimFuncref(
  val handler: FunctionHandler,
  val arguments: VimList,
  val dictionary: VimDictionary,
  val type: Type,
) : VimDataType() {

  var isSelfFixed = false

  companion object {
    var lambdaCounter = 0
  }

  override fun asDouble(): Double {
    throw ExException("E703: using Funcref as a Number")
  }

  override fun asString(): String {
    throw ExException("E729: using Funcref as a String")
  }

  override fun toString(): String {
    return if (arguments.values.isEmpty()) {
      when (type) {
        Type.LAMBDA -> "function('${handler.name}')"
        Type.FUNCREF -> "function('${handler.name}')"
        Type.FUNCTION -> handler.name
      }
    } else {
      "function('${handler.name}', $arguments)"
    }
  }

  override fun toVimNumber(): VimInt {
    throw ExException("E703: using Funcref as a Number")
  }

  fun execute(args: List<Expression>, editor: Editor, context: DataContext, parent: Executable): VimDataType {
    val allArguments = listOf(this.arguments.values.map { SimpleExpression(it) }, args).flatten()
      if (handler is DefinedFunctionHandler && handler.function.isDeleted) {
        throw ExException("E933: Function was deleted: ${handler.name}")
      }
      val handler = when (type) {
        Type.LAMBDA, Type.FUNCREF -> this.handler
        Type.FUNCTION -> {
          FunctionStorage.getFunctionHandlerOrNull(handler.scope, handler.name, parent)
            ?: throw ExException("E117: Unknown function: ${handler.name}")
        }
      }
      return handler.executeFunction(allArguments, editor, context, parent)
    }

    enum class Type {
      LAMBDA,
      FUNCREF,
      FUNCTION,
    }
  }
  