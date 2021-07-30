package com.maddyhome.idea.vim.vimscript.model.statements

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.services.PatternService

data class TryStatement(val tryBlock: TryBlock, val catchBlocks: List<CatchBlock>, val finallyBlock: FinallyBlock?) :
  Executable {

  override fun execute(
    editor: Editor?,
    context: DataContext?,
    vimContext: VimContext,
    skipHistory: Boolean,
  ): ExecutionResult {
    var uncaughtException: ExException? = null
    var result: ExecutionResult = ExecutionResult.Success
    try {
      result = tryBlock.execute(editor, context, vimContext)
      if (result !is ExecutionResult.Success) {
        return result
      }
    } catch (e: ExException) {
      var caught = false
      for (catchBlock in catchBlocks) {
        if (PatternService.matches(catchBlock.pattern, e.message)) {
          caught = true
          result = catchBlock.execute(editor, context, vimContext)
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
      result = finallyBlock.execute(editor, context, vimContext)
    }
    if (uncaughtException != null) {
      throw uncaughtException
    }
    return result
  }
}

data class TryBlock(val body: List<Executable>) : Executable {
  override fun execute(
    editor: Editor?,
    context: DataContext?,
    vimContext: VimContext,
    skipHistory: Boolean,
  ): ExecutionResult {
    return executeBody(body, editor, context, vimContext)
  }
}

data class CatchBlock(val pattern: String, val body: List<Executable>) : Executable {
  override fun execute(
    editor: Editor?,
    context: DataContext?,
    vimContext: VimContext,
    skipHistory: Boolean,
  ): ExecutionResult {
    return executeBody(body, editor, context, vimContext)
  }
}

data class FinallyBlock(val body: List<Executable>) : Executable {
  override fun execute(
    editor: Editor?,
    context: DataContext?,
    vimContext: VimContext,
    skipHistory: Boolean,
  ): ExecutionResult {
    return executeBody(body, editor, context, vimContext)
  }
}

private fun executeBody(
  body: List<Executable>,
  editor: Editor?,
  context: DataContext?,
  vimContext: VimContext,
): ExecutionResult {
  var result: ExecutionResult = ExecutionResult.Success
  for (statement in body) {
    if (result is ExecutionResult.Success) {
      result = statement.execute(editor, context, vimContext)
    } else {
      break
    }
  }
  return result
}
