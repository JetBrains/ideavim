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
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression

data class IfStatement(val conditionToBody: List<Pair<Expression, List<Executable>>>) : Executable {
  override lateinit var parent: Executable

  override fun execute(editor: Editor, context: DataContext): ExecutionResult {
    var result: ExecutionResult = ExecutionResult.Success
    var statementsToExecute: List<Executable>? = null
    for ((condition, statements) in conditionToBody) {
      if (condition.evaluate(editor, context, this).asBoolean()) {
        statementsToExecute = statements
        statementsToExecute.forEach { it.parent = this }
        break
      }
    }
    if (statementsToExecute != null) {
      var exception: Exception? = null
      for (statement in statementsToExecute) {
        if (result is ExecutionResult.Success) {
          // todo delete try block after Result class
          try {
            result = statement.execute(editor, context)
          } catch (e: Exception) {
            exception = e
          }
        } else {
          break
        }
      }
      if (exception != null) {
        throw exception
      }
    }
    return result
  }
}
