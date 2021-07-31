package com.maddyhome.idea.vim.vimscript.model.statements.loops

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.services.VariableService

data class ForLoop(val variable: String, val iterable: Expression, val body: List<Executable>) : Executable {

  // todo refactoring
  override fun execute(editor: Editor?, context: DataContext?, vimContext: VimContext): ExecutionResult {
    var result: ExecutionResult = ExecutionResult.Success
    var iterableValue = iterable.evaluate(editor, context, vimContext)
    if (iterableValue is VimString) {
      for (i in iterableValue.value) {
        VariableService.storeVariable(Variable(null, variable), VimString(i.toString()), editor, context, vimContext)
        for (statement in body) {
          if (result is ExecutionResult.Success) {
            result = statement.execute(editor, context, vimContext)
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
        }
      }
    } else if (iterableValue is VimList) {
      var index = 0
      while (index < (iterableValue as VimList).values.size) {
        VariableService.storeVariable(
          Variable(null, variable),
          iterableValue.values[index],
          editor,
          context,
          vimContext
        )
        for (statement in body) {
          if (result is ExecutionResult.Success) {
            result = statement.execute(editor, context, vimContext)
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
        }
        index += 1
        iterableValue = iterable.evaluate(editor, context, vimContext) as VimList
      }
    } else if (iterableValue is VimBlob) {
      TODO("Not yet implemented")
    } else {
      throw ExException("E714: List required")
    }
    return result
  }
}
