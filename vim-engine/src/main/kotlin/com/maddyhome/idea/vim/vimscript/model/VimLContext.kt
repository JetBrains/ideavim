/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model

import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration

public sealed interface VimLContext {

  // todo rename
  public fun getPreviousParentContext(): VimLContext

  public fun isFirstParentContext(): Boolean {
    return this is Script || this is CommandLineVimLContext
  }

  // todo rename
  // todo documentation
  public fun getFirstParentContext(): VimLContext {
    return if (isFirstParentContext()) {
      this
    } else {
      val previousContext = this.getPreviousParentContext()
      previousContext.getFirstParentContext()
    }
  }

  // todo better name
  public fun getExecutableContext(executable: VimLContext): ExecutableContext {
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

  public fun getScript(): Script? {
    val firstParentContext = getFirstParentContext()
    return if (firstParentContext is Script) firstParentContext else null
  }
}

/*
 * VimL that was invoked from command line
 */
public object CommandLineVimLContext : VimLContext {

  override fun getPreviousParentContext(): VimLContext {
    throw RuntimeException("Command line has no parent context")
  }
}

// todo rename
public enum class ExecutableContext {
  COMMAND_LINE,
  SCRIPT,
  FUNCTION
}
