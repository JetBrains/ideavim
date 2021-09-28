package com.maddyhome.idea.vim.vimscript.model

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag

interface Executable {

  var parent: Executable

  fun execute(editor: Editor, context: DataContext): ExecutionResult

  fun getContext(): ExecutableContext {
    var node: Executable? = this
    while (node !is FunctionDeclaration && node !is Script) {
      if (node != null) {
        node = node.parent
      } else {
        throw RuntimeException("Object of ${this::class.java} class has no parent")
      }
    }
    if (node is FunctionDeclaration) {
      return ExecutableContext.FUNCTION
    }
    return ExecutableContext.SCRIPT
  }

  fun getScript(): Script {
    var node: Executable? = this
    while (node !is Script && node != null) {
      node = node.parent
    }
    if (node == null) {
      throw RuntimeException("Object of ${this::class.java} class has no parent script")
    }
    return node as Script
  }

  fun getFunction(closure: Boolean): FunctionDeclaration? {
    var node: Executable? = this
    while (node != null) {
      if (node is FunctionDeclaration) {
        if (!node.flags.contains(FunctionFlag.CLOSURE)) {
          break
        } else if (node.flags.contains(FunctionFlag.CLOSURE) && closure) {
          break
        }
      }
      node = node.parent
    }
    if (node is FunctionDeclaration) {
      return node
    }
    return null
  }
}

enum class ExecutableContext {
  SCRIPT,
  FUNCTION
}
