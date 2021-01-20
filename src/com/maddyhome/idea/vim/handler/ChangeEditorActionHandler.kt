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

package com.maddyhome.idea.vim.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.change.VimRepeater
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.helper.vimChangeActionSwitchMode
import com.maddyhome.idea.vim.helper.vimLastColumn

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
    abstract fun execute(editor: Editor, caret: Caret, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Boolean
  }

  /**
   * This handler executes an action only once for all carets. That means that if you have 5 carets,
   *   [execute] will be called 1 time.
   * @see [ChangeEditorActionHandler.ForEachCaret] for per-caret execution
   */
  abstract class SingleExecution : ChangeEditorActionHandler() {
    abstract fun execute(editor: Editor, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Boolean
  }

  final override fun baseExecute(editor: Editor, caret: Caret, context: DataContext, cmd: Command): Boolean {
    // Here we have to save the last changed command. This should be done separately for each
    // call of the task, not for each caret. Currently there is no way to schedule any action
    // to be worked after each task. So here we override the deprecated execute function which
    // is called for each task and call the handlers for each caret, if implemented.

    editor.vimChangeActionSwitchMode = null

    val worked = Ref.create(true)
    when (this) {
      is ForEachCaret -> {
        editor.caretModel.runForEachCaret({ current ->
          if (!current.isValid) return@runForEachCaret
          if (!execute(editor, current, context, cmd.count, cmd.rawCount, cmd.argument)) {
            worked.set(false)
          }
        }, true)
      }
      is SingleExecution -> {
        worked.set(execute(editor, context, cmd.count, cmd.rawCount, cmd.argument))
      }
    }

    if (worked.get()) {
      VimRepeater.saveLastChange(cmd)
      VimRepeater.repeatHandler = false
      editor.caretModel.allCarets.forEach { it.vimLastColumn = it.visualPosition.column }
    }

    val toSwitch = editor.vimChangeActionSwitchMode
    if (toSwitch != null) {
      VimPlugin.getChange().processPostChangeModeSwitch(editor, context, toSwitch)
    }

    return worked.get()
  }
}
