package com.maddyhome.idea.vim.vimscript.model.statements

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.services.PatternService

data class TryStatement(val tryBlock: TryBlock, val catchBlocks: List<CatchBlock>, val finallyBlock: FinallyBlock?) : Executable() {

  override fun execute(editor: Editor, context: DataContext): ExecutionResult {
    var uncaughtException: ExException? = null
    var result: ExecutionResult = ExecutionResult.Success
    try {
      tryBlock.parent = this
      result = tryBlock.execute(editor, context)
      if (result !is ExecutionResult.Success) {
        return result
      }
    } catch (e: ExException) {
      var caught = false
      for (catchBlock in catchBlocks) {
        catchBlock.parent = this
        if (PatternService.matches(catchBlock.pattern, e.message)) {
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
      finallyBlock.parent = this
      result = finallyBlock.execute(editor, context)
    }
    if (uncaughtException != null) {
      throw uncaughtException
    }
    return result
  }
}

data class TryBlock(val body: List<Executable>) : Executable() {
  override fun execute(editor: Editor, context: DataContext): ExecutionResult {
    body.forEach { it.parent = this.parent }
    return executeBody(body, editor, context)
  }
}

data class CatchBlock(val pattern: String, val body: List<Executable>) : Executable() {
  override fun execute(editor: Editor, context: DataContext): ExecutionResult {
    body.forEach { it.parent = this.parent }
    return executeBody(body, editor, context)
  }
}

data class FinallyBlock(val body: List<Executable>) : Executable() {
  override fun execute(editor: Editor, context: DataContext): ExecutionResult {
    body.forEach { it.parent = this.parent }
    return executeBody(body, editor, context)
  }
}

fun executeBody(
  body: List<Executable>,
  editor: Editor,
  context: DataContext,
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
