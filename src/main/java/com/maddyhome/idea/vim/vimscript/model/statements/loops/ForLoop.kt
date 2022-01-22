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

package com.maddyhome.idea.vim.vimscript.model.statements.loops

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable

// todo refactor us senpai :(
data class ForLoop(val variable: Variable, val iterable: Expression, val body: List<Executable>) : Executable {
  override lateinit var parent: VimLContext

  override fun execute(editor: Editor, context: DataContext): ExecutionResult {
    var result: ExecutionResult = ExecutionResult.Success
    body.forEach { it.parent = this }

    var iterableValue = iterable.evaluate(editor, context, this)
    if (iterableValue is VimString) {
      for (i in iterableValue.value) {
        VimPlugin.getVariableService().storeVariable(variable, VimString(i.toString()), editor, context, this)
        for (statement in body) {
          if (result is ExecutionResult.Success) {
            result = statement.execute(editor, context)
          } else {
            break
          }
        }
        if (result is ExecutionResult.Break) {
          result = ExecutionResult.Success
          break
        } else if (result is ExecutionResult.Continue) {
          result = ExecutionResult.Success
          continue
        } else if (result is ExecutionResult.Error) {
          break
        }
      }
    } else if (iterableValue is VimList) {
      var index = 0
      while (index < (iterableValue as VimList).values.size) {
        VimPlugin.getVariableService().storeVariable(variable, iterableValue.values[index], editor, context, this)
        for (statement in body) {
          if (result is ExecutionResult.Success) {
            result = statement.execute(editor, context)
          } else {
            break
          }
        }
        if (result is ExecutionResult.Break) {
          result = ExecutionResult.Success
          break
        } else if (result is ExecutionResult.Continue) {
          result = ExecutionResult.Success
          continue
        } else if (result is ExecutionResult.Error) {
          break
        }
        index += 1
        iterableValue = iterable.evaluate(editor, context, this) as VimList
      }
    } else if (iterableValue is VimBlob) {
      TODO("Not yet implemented")
    } else {
      throw ExException("E1098: String, List or Blob required")
    }
    return result
  }
}

data class ForLoopWithList(val variables: List<String>, val iterable: Expression, val body: List<Executable>) : Executable {
  override lateinit var parent: VimLContext

  override fun execute(editor: Editor, context: DataContext): ExecutionResult {
    var result: ExecutionResult = ExecutionResult.Success
    body.forEach { it.parent = this }

    var iterableValue = iterable.evaluate(editor, context, this)
    if (iterableValue is VimList) {
      var index = 0
      while (index < (iterableValue as VimList).values.size) {
        storeListVariables(iterableValue.values[index], editor, context)
        for (statement in body) {
          if (result is ExecutionResult.Success) {
            result = statement.execute(editor, context)
          } else {
            break
          }
        }
        if (result is ExecutionResult.Break) {
          result = ExecutionResult.Success
          break
        } else if (result is ExecutionResult.Continue) {
          result = ExecutionResult.Success
          continue
        } else if (result is ExecutionResult.Error) {
          break
        }
        index += 1
        iterableValue = iterable.evaluate(editor, context, this) as VimList
      }
    } else {
      throw ExException("E714: List required")
    }
    return result
  }

  private fun storeListVariables(list: VimDataType, editor: Editor, context: DataContext) {
    if (list !is VimList) {
      throw ExException("E714: List required")
    }

    if (list.values.size < variables.size) {
      throw ExException("E688: More targets than List items")
    }
    if (list.values.size > variables.size) {
      throw ExException("E684: Less targets than List items")
    }

    for (item in list.values.withIndex()) {
      VimPlugin.getVariableService().storeVariable(Variable(null, variables[item.index]), item.value, editor, context, this)
    }
  }
}
