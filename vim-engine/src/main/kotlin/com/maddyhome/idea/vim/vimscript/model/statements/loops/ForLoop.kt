/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.statements.loops

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
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
import com.maddyhome.idea.vim.vimscript.parser.DeletionInfo

// todo refactor us senpai :(
data class ForLoop(val variable: Variable, val iterable: Expression, val body: List<Executable>) : Executable {
  override lateinit var vimContext: VimLContext
  override lateinit var rangeInScript: TextRange

  override fun execute(editor: VimEditor, context: ExecutionContext): ExecutionResult {
    injector.statisticsService.setIfLoopUsed(true)
    var result: ExecutionResult = ExecutionResult.Success
    body.forEach { it.vimContext = this }

    var iterableValue = iterable.evaluate(editor, context, this)
    if (iterableValue is VimString) {
      for (i in iterableValue.value) {
        injector.variableService.storeVariable(variable, VimString(i.toString()), editor, context, this)
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
        injector.variableService.storeVariable(variable, iterableValue.values[index], editor, context, this)
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

  override fun restoreOriginalRange(deletionInfo: DeletionInfo) {
    super.restoreOriginalRange(deletionInfo)
    body.forEach { it.restoreOriginalRange(deletionInfo) }
  }
}

data class ForLoopWithList(val variables: List<String>, val iterable: Expression, val body: List<Executable>) :
  Executable {
  override lateinit var vimContext: VimLContext
  override lateinit var rangeInScript: TextRange

  override fun execute(editor: VimEditor, context: ExecutionContext): ExecutionResult {
    var result: ExecutionResult = ExecutionResult.Success
    body.forEach { it.vimContext = this }

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

  private fun storeListVariables(list: VimDataType, editor: VimEditor, context: ExecutionContext) {
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
      injector.variableService.storeVariable(Variable(null, variables[item.index]), item.value, editor, context, this)
    }
  }

  override fun restoreOriginalRange(deletionInfo: DeletionInfo) {
    super.restoreOriginalRange(deletionInfo)
    body.forEach { it.restoreOriginalRange(deletionInfo) }
  }
}
