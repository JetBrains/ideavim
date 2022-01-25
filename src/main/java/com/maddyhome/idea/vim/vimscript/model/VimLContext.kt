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

package com.maddyhome.idea.vim.vimscript.model

import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration

sealed interface VimLContext {

  // todo rename
  fun getPreviousParentContext(): VimLContext

  fun isFirstParentContext(): Boolean {
    return this is Script || this is CommandLineVimLContext
  }

  // todo rename
  // todo documentation
  fun getFirstParentContext(): VimLContext {
    return if (isFirstParentContext()) {
      this
    } else {
      val previousContext = this.getPreviousParentContext()
      previousContext.getFirstParentContext()
    }
  }

  // todo better name
  fun getExecutableContext(executable: VimLContext): ExecutableContext {
    var currentNode: VimLContext = executable
    while (currentNode !is FunctionDeclaration && !currentNode.isFirstParentContext()) {
      currentNode = currentNode.getPreviousParentContext()
    }
    return when (currentNode) {
      is FunctionDeclaration -> ExecutableContext.FUNCTION
      is Script -> ExecutableContext.SCRIPT
      is CommandLineVimLContext -> ExecutableContext.COMMAND_LINE
      else -> throw RuntimeException("Reached unknown first parent context")
    }
  }

  fun getScript(): Script? {
    val firstParentContext = getFirstParentContext()
    return if (firstParentContext is Script) firstParentContext else null
  }
}

/*
 * VimL that was invoked from command line
 */
object CommandLineVimLContext : VimLContext {

  override fun getPreviousParentContext(): VimLContext {
    throw RuntimeException("Command line has no parent context")
  }
}

// todo rename
enum class ExecutableContext {
  COMMAND_LINE,
  SCRIPT,
  FUNCTION
}
