/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.handler

import com.maddyhome.idea.vim.action.change.VimRepeater
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments

/**
 * Base handler for commands that performs change actions.
 * This handler stores the commands and they can be repeated later with dot command.
 *
 * Use subclasses of this handler:
 *   - [ChangeEditorActionHandler.SingleExecution]
 *   - [ChangeEditorActionHandler.ForEachCaret]
 */
sealed class ChangeEditorActionHandler(runForEachCaret: Boolean) : EditorActionHandlerBase(runForEachCaret) {

  /**
   * This handler executes an action for each caret. That means that if you have 5 carets, [execute] will be
   *   called 5 times.
   * @see [ChangeEditorActionHandler.SingleExecution] for only one execution.
   */
  abstract class ForEachCaret : ChangeEditorActionHandler(true) {
    abstract fun execute(
      editor: VimEditor,
      caret: VimCaret,
      context: ExecutionContext,
      argument: Argument?,
      operatorArguments: OperatorArguments,
    ): Boolean
  }

  /**
   * This handler executes an action only once for all carets. That means that if you have 5 carets,
   *   [execute] will be called 1 time.
   * @see [ChangeEditorActionHandler.ForEachCaret] for per-caret execution
   */
  abstract class SingleExecution : ChangeEditorActionHandler(false) {
    abstract fun execute(
      editor: VimEditor,
      context: ExecutionContext,
      argument: Argument?,
      operatorArguments: OperatorArguments,
    ): Boolean
  }

  abstract class ConditionalSingleExecution : ChangeEditorActionHandler(true) {
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
      argument: Argument?,
      operatorArguments: OperatorArguments,
    ): Boolean

    abstract fun execute(
      editor: VimEditor,
      context: ExecutionContext,
      argument: Argument?,
      operatorArguments: OperatorArguments,
    ): Boolean
  }

  private val worked = arrayOf(true)

  final override fun baseExecute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    // Here we have to save the last changed command. This should be done separately for each
    // call of the task, not for each caret. Currently there is no way to schedule any action
    // to be worked after each task. So here we override the deprecated execute function which
    // is called for each task and call the handlers for each caret, if implemented.

    // Shouldn't we just use [EditorWriteActionHandler]?
    editor.vimChangeActionSwitchMode = null

    editor.startGuardedBlockChecking()
    worked[0] = true
    try {
      when (this) {
        is ForEachCaret -> {
          if (!execute(editor, caret, context, cmd.argument, operatorArguments)) {
            worked[0] = false
          }
        }

        is SingleExecution -> {
          worked[0] = execute(editor, context, cmd.argument, operatorArguments)
        }

        is ConditionalSingleExecution -> {
          // The handler is registered as multicaret run. So, if we want to execute the handler once, we call
          //   it only on main caret iteration.
          val runAsMulticaret = this.runAsMulticaret(editor, context, cmd, operatorArguments)
          if (runAsMulticaret) {
            if (!execute(editor, caret, context, cmd.argument, operatorArguments)) {
              worked[0] = false
            }
          } else {
            if (caret == editor.currentCaret()) {
              worked[0] = execute(editor, context, cmd.argument, operatorArguments)
            }
          }
        }
      }
    } catch (e: java.lang.Exception) {
      if (injector.application.isUnitTest() || e.javaClass.name != "ReadOnlyFragmentModificationException") {
        throw e
      } else {
        injector.engineEditorHelper.handleWithReadonlyFragmentModificationHandler(editor, e)
      }
    } finally {
      editor.stopGuardedBlockChecking()
    }

    return worked[0]
  }

  final override fun postExecute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ) {
    if (worked[0]) {
      VimRepeater.saveLastChange(cmd)
      VimRepeater.repeatHandler = false
    }

    val toSwitch = editor.vimChangeActionSwitchMode
    if (toSwitch != null) {
      injector.changeGroup.processPostChangeModeSwitch(editor, context, toSwitch)
    }
  }
}
