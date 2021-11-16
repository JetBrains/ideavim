/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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
