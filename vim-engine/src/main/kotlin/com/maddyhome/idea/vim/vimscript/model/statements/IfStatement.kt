/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.statements

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.parser.DeletionInfo

data class IfStatement(val conditionToBody: List<Pair<Expression, List<Executable>>>) : Executable {
  override lateinit var vimContext: VimLContext
  override lateinit var rangeInScript: TextRange

  override fun execute(editor: VimEditor, context: ExecutionContext): ExecutionResult {
    injector.statisticsService.setIfIfUsed(true)
    var result: ExecutionResult = ExecutionResult.Success
    var statementsToExecute: List<Executable>? = null
    for ((condition, statements) in conditionToBody) {
      if (condition.evaluate(editor, context, this).toVimNumber().booleanValue) {
        statementsToExecute = statements
        statementsToExecute.forEach { it.vimContext = this }
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

  override fun restoreOriginalRange(deletionInfo: DeletionInfo) {
    super.restoreOriginalRange(deletionInfo)
    conditionToBody.flatMap { it.second }.forEach { it.restoreOriginalRange(deletionInfo) }
  }
}
