/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package com.maddyhome.idea.vim.vimscript.model.statements

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.services.FunctionStorage

data class FunctionDeclaration(
  val scope: Scope?,
  val name: String,
  val args: List<String>,
  val defaultArgs: List<Pair<String, Expression>>,
  val body: List<Executable>,
  val replaceExisting: Boolean,
  val flags: Set<FunctionFlag>,
  val hasOptionalArguments: Boolean,
) : Executable {
  override lateinit var parent: Executable
  var isDeleted = false

  /**
   * we store the "a:" and "l:" scope variables here
   * see ":h scope"
   */
  val functionVariables: MutableMap<String, VimDataType> = mutableMapOf()
  val localVariables: MutableMap<String, VimDataType> = mutableMapOf()

  override fun execute(editor: Editor, context: DataContext): ExecutionResult {
    val forbiddenArgumentNames = setOf("firstline", "lastline")
    val forbiddenArgument = args.firstOrNull { forbiddenArgumentNames.contains(it) }
    if (forbiddenArgument != null) {
      throw ExException("E125: Illegal argument: $forbiddenArgument")
    }

    body.forEach { it.parent = this }
    FunctionStorage.storeFunction(this)
    return ExecutionResult.Success
  }
}

enum class FunctionFlag(val abbrev: String) {
  RANGE("range"),
  ABORT("abort"),
  DICT("dict"),
  CLOSURE("closure");

  companion object {
    fun getByName(abbrev: String): FunctionFlag? {
      return values().firstOrNull { it.abbrev == abbrev }
    }
  }
}
