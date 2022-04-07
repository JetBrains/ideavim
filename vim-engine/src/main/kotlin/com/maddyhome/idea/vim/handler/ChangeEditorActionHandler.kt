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
sealed class ChangeEditorActionHandler : EditorActionHandlerBase(false) {

  /**
   * This handler executes an action for each caret. That means that if you have 5 carets, [execute] will be
   *   called 5 times.
   * @see [ChangeEditorActionHandler.SingleExecution] for only one execution.
   */
  abstract class ForEachCaret : ChangeEditorActionHandler() {
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
  abstract class SingleExecution : ChangeEditorActionHandler() {
    abstract fun execute(
      editor: VimEditor,
      context: ExecutionContext,
      argument: Argument?,
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
    // Here we have to save the last changed command. This should be done separately for each
    // call of the task, not for each caret. Currently there is no way to schedule any action
    // to be worked after each task. So here we override the deprecated execute function which
    // is called for each task and call the handlers for each caret, if implemented.

    // Shouldn't we just use [EditorWriteActionHandler]?
    editor.vimChangeActionSwitchMode = null

    editor.startGuardedBlockChecking()

    val worked = arrayOf(true)
    try {
      when (this) {
        is ForEachCaret -> {
          editor.forEachNativeCaret(
            { current ->
              if (!current.isValid) return@forEachNativeCaret
              if (!execute(editor, current, context, cmd.argument, operatorArguments)) {
                worked[0] = false
              }
            },
            true
          )
        }
        is SingleExecution -> {
          worked[0] = execute(editor, context, cmd.argument, operatorArguments)
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

    if (worked[0]) {
      VimRepeater.saveLastChange(cmd)
      VimRepeater.repeatHandler = false
      editor.forEachNativeCaret({ it.vimLastColumn = it.getVisualPosition().column })
    }

    val toSwitch = editor.vimChangeActionSwitchMode
    if (toSwitch != null) {
      injector.changeGroup.processPostChangeModeSwitch(editor, context, toSwitch)
    }

    return worked[0]
  }
}
