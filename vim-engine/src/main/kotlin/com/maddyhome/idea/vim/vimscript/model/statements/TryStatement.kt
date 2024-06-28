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
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.FinishException
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.parser.DeletionInfo

data class TryStatement(val tryBlock: TryBlock, val catchBlocks: List<CatchBlock>, val finallyBlock: FinallyBlock?) :
  Executable {
  override lateinit var vimContext: VimLContext
  override lateinit var rangeInScript: TextRange

  override fun execute(editor: VimEditor, context: ExecutionContext): ExecutionResult {
    var uncaughtException: ExException? = null
    var result: ExecutionResult = ExecutionResult.Success
    try {
      tryBlock.vimContext = this
      result = tryBlock.execute(editor, context)
      if (result !is ExecutionResult.Success) {
        return result
      }
    } catch (e: ExException) {
      if (e is FinishException) {
        if (finallyBlock != null) {
          finallyBlock.vimContext = this
          finallyBlock.execute(editor, context)
        }
        throw e
      }

      var caught = false
      for (catchBlock in catchBlocks) {
        catchBlock.vimContext = this
        if (injector.regexpService.matches(catchBlock.pattern, e.message)) {
          caught = true
          result = catchBlock.execute(editor, context)
          if (result !is ExecutionResult.Success) {
            return result
          }
          break
        }
      }
      if (!caught) {
        uncaughtException = e
      }
    }
    if (finallyBlock != null) {
      finallyBlock.vimContext = this
      result = finallyBlock.execute(editor, context)
    }
    if (uncaughtException != null) {
      throw uncaughtException
    }
    return result
  }

  override fun restoreOriginalRange(deletionInfo: DeletionInfo) {
    super.restoreOriginalRange(deletionInfo)
    tryBlock.restoreOriginalRange(deletionInfo)
    catchBlocks.forEach { it.restoreOriginalRange(deletionInfo) }
    finallyBlock?.restoreOriginalRange(deletionInfo)
  }
}

data class TryBlock(val body: List<Executable>) : Executable {
  override lateinit var vimContext: VimLContext
  override lateinit var rangeInScript: TextRange
  override fun execute(editor: VimEditor, context: ExecutionContext): ExecutionResult {
    body.forEach { it.vimContext = this.vimContext }
    return executeBody(body, editor, context)
  }

  override fun restoreOriginalRange(deletionInfo: DeletionInfo) {
    super.restoreOriginalRange(deletionInfo)
    body.forEach { it.restoreOriginalRange(deletionInfo) }
  }
}

data class CatchBlock(val pattern: String, val body: List<Executable>) : Executable {
  override lateinit var vimContext: VimLContext
  override lateinit var rangeInScript: TextRange
  override fun execute(editor: VimEditor, context: ExecutionContext): ExecutionResult {
    body.forEach { it.vimContext = this.vimContext }
    return executeBody(body, editor, context)
  }

  override fun restoreOriginalRange(deletionInfo: DeletionInfo) {
    super.restoreOriginalRange(deletionInfo)
    body.forEach { it.restoreOriginalRange(deletionInfo) }
  }
}

data class FinallyBlock(val body: List<Executable>) : Executable {
  override lateinit var vimContext: VimLContext
  override lateinit var rangeInScript: TextRange
  override fun execute(editor: VimEditor, context: ExecutionContext): ExecutionResult {
    body.forEach { it.vimContext = this.vimContext }
    return executeBody(body, editor, context)
  }

  override fun restoreOriginalRange(deletionInfo: DeletionInfo) {
    super.restoreOriginalRange(deletionInfo)
    body.forEach { it.restoreOriginalRange(deletionInfo) }
  }
}

fun executeBody(
  body: List<Executable>,
  editor: VimEditor,
  context: ExecutionContext,
): ExecutionResult {
  var result: ExecutionResult = ExecutionResult.Success
  for (statement in body) {
    if (result is ExecutionResult.Success) {
      result = statement.execute(editor, context)
    } else {
      break
    }
  }
  return result
}
