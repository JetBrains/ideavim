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

package com.maddyhome.idea.vim.vimscript.model.functions.handlers

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler
import com.maddyhome.idea.vim.vimscript.services.FunctionStorage

object FunctionFunctionHandler : FunctionHandler() {
  override val name = "function"
  override val minimumNumberOfArguments: Int = 1
  override val maximumNumberOfArguments: Int = 3

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: Editor,
    context: DataContext,
    parent: Executable,
  ): VimFuncref {
    val arg1 = argumentValues[0].evaluate(editor, context, parent)
    if (arg1 !is VimString) {
      throw ExException("E129: Function name required")
    }
    val scopeAndName = arg1.value.extractScopeAndName()
    val function = FunctionStorage.getFunctionHandlerOrNull(scopeAndName.first, scopeAndName.second, parent)
      ?: throw ExException("E700: Unknown function: ${if (scopeAndName.first != null) scopeAndName.first!!.c + ":" else ""}${scopeAndName.second}")

    var arglist: VimList? = null
    var dictionary: VimDictionary? = null
    val arg2 = argumentValues.getOrNull(1)?.evaluate(editor, context, parent)
    val arg3 = argumentValues.getOrNull(2)?.evaluate(editor, context, parent)

    if (arg2 is VimDictionary && arg3 is VimDictionary) {
      throw ExException("E923: Second argument of function() must be a list or a dict")
    }

    if (arg2 != null) {
      when (arg2) {
        is VimList -> arglist = arg2
        is VimDictionary -> dictionary = arg2
        else -> throw ExException("E923: Second argument of function() must be a list or a dict")
      }
    }

    if (arg3 != null && arg3 !is VimDictionary) {
      throw ExException("E922: expected a dict")
    }
    return VimFuncref(function, arglist ?: VimList(mutableListOf()), dictionary ?: VimDictionary(LinkedHashMap()), VimFuncref.Type.FUNCTION)
  }
}

object FuncrefFunctionHandler : FunctionHandler() {
  override val name = "function"
  override val minimumNumberOfArguments: Int = 1
  override val maximumNumberOfArguments: Int = 3

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: Editor,
    context: DataContext,
    parent: Executable,
  ): VimFuncref {
    val arg1 = argumentValues[0].evaluate(editor, context, parent)
    if (arg1 !is VimString) {
      throw ExException("E129: Function name required")
    }
    val scopeAndName = arg1.value.extractScopeAndName()
    val function = FunctionStorage.getUserDefinedFunction(scopeAndName.first, scopeAndName.second, parent)
      ?: throw ExException("E700: Unknown function: ${if (scopeAndName.first != null) scopeAndName.first!!.c + ":" else ""}${scopeAndName.second}")
    val handler = DefinedFunctionHandler(function)

    var arglist: VimList? = null
    var dictionary: VimDictionary? = null
    val arg2 = argumentValues.getOrNull(1)?.evaluate(editor, context, parent)
    val arg3 = argumentValues.getOrNull(2)?.evaluate(editor, context, parent)

    if (arg2 is VimDictionary && arg3 is VimDictionary) {
      throw ExException("E923: Second argument of function() must be a list or a dict")
    }

    if (arg2 != null) {
      when (arg2) {
        is VimList -> arglist = arg2
        is VimDictionary -> dictionary = arg2
        else -> throw ExException("E923: Second argument of function() must be a list or a dict")
      }
    }

    if (arg3 != null && arg3 !is VimDictionary) {
      throw ExException("E922: expected a dict")
    }
    return VimFuncref(handler, arglist ?: VimList(mutableListOf()), dictionary ?: VimDictionary(LinkedHashMap()), VimFuncref.Type.FUNCREF)
  }
}

private fun String.extractScopeAndName(): Pair<Scope?, String> {
  val colonIndex = this.indexOf(":")
  if (colonIndex == -1) {
    return Pair(null, this)
  }
  val scopeString = this.substring(0, colonIndex)
  val nameString = this.substring(colonIndex + 1)
  return Pair(Scope.getByValue(scopeString), nameString)
}
