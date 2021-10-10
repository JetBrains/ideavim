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
import com.maddyhome.idea.vim.command.Command

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
    abstract fun execute(editor: Editor, caret: Caret, context: DataContext, cmd: Command): Boolean
  }

  /**
   * This handler executes an action only once for all carets. That means that if you have 5 carets,
   *   [execute] will be called 1 time.
   * @see [VimActionHandler.ForEachCaret] for per-caret execution.
   */
  abstract class SingleExecution : VimActionHandler(false) {
    abstract fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean
  }

  final override fun baseExecute(editor: Editor, caret: Caret, context: DataContext, cmd: Command): Boolean {
    return when (this) {
      is ForEachCaret -> execute(editor, caret, context, cmd)
      is SingleExecution -> execute(editor, context, cmd)
    }
  }
}
