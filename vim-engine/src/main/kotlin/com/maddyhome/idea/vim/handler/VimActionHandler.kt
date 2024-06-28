/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.handler

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments

/**
 * Handler for common usage.
 *
 * Use subclasses of this handler:
 *   - [VimActionHandler.SingleExecution]
 *   - [VimActionHandler.ForEachCaret]
 */
sealed class VimActionHandler(myRunForEachCaret: Boolean) : EditorActionHandlerBase(myRunForEachCaret) {
  /**
   * This handler executes an action for each caret. That means that if you have 5 carets,
   *   [execute] will be called 5 times.
   * @see [VimActionHandler.SingleExecution] for only one execution.
   */
  abstract class ForEachCaret : VimActionHandler(true) {
    abstract fun execute(
      editor: VimEditor,
      caret: VimCaret,
      context: ExecutionContext,
      cmd: Command,
      operatorArguments: OperatorArguments,
    ): Boolean
  }

  /**
   * This handler executes an action only once for all carets. That means that if you have 5 carets,
   *   [execute] will be called 1 time.
   * @see [VimActionHandler.ForEachCaret] for per-caret execution.
   */
  abstract class SingleExecution : VimActionHandler(false) {
    abstract fun execute(
      editor: VimEditor,
      context: ExecutionContext,
      cmd: Command,
      operatorArguments: OperatorArguments,
    ): Boolean
  }

  abstract class ConditionalMulticaret : VimActionHandler(false) {
    abstract fun runAsMulticaret(
      editor: VimEditor,
      context: ExecutionContext,
      cmd: Command,
      operatorArguments: OperatorArguments,
    ): Boolean

    abstract fun execute(
      editor: VimEditor,
      caret: VimCaret,
      context: ExecutionContext,
      cmd: Command,
      operatorArguments: OperatorArguments,
    ): Boolean

    abstract fun execute(
      editor: VimEditor,
      context: ExecutionContext,
      cmd: Command,
      operatorArguments: OperatorArguments,
    ): Boolean
  }

  final override fun baseExecute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    return when (this) {
      is ForEachCaret -> execute(editor, caret, context, cmd, operatorArguments)
      is SingleExecution -> execute(editor, context, cmd, operatorArguments)
      is ConditionalMulticaret -> {
        val runAsMulticaret = runAsMulticaret(editor, context, cmd, operatorArguments)
        return if (runAsMulticaret) {
          var res = true
          editor.forEachCaret { res = execute(editor, it, context, cmd, operatorArguments) && res }
          res
        } else {
          execute(editor, context, cmd, operatorArguments)
        }
      }
    }
  }
}
