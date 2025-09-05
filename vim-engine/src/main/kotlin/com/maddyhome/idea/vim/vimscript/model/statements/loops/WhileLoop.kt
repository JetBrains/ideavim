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
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.parser.DeletionInfo

data class WhileLoop(val condition: Expression, val body: List<Executable>) : Executable {
  override lateinit var vimContext: VimLContext
  override lateinit var rangeInScript: TextRange

  override fun execute(editor: VimEditor, context: ExecutionContext): ExecutionResult {
    injector.statisticsService.setIfLoopUsed(true)
    var result: ExecutionResult = ExecutionResult.Success
    body.forEach { it.vimContext = this }

    while (condition.evaluate(editor, context, this).toVimNumber().booleanValue) {
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
    return result
  }

  override fun restoreOriginalRange(deletionInfo: DeletionInfo) {
    super.restoreOriginalRange(deletionInfo)
    body.forEach { it.restoreOriginalRange(deletionInfo) }
  }
}
