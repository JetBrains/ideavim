/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

sealed class ChangeEditorActionHandler : VimActionHandler.SingleExecution() {

  abstract class ForEachCaret : ChangeEditorActionHandler() {
    abstract fun execute(editor: Editor, caret: Caret, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Boolean
  }

  abstract class SingleExecution : ChangeEditorActionHandler() {
    abstract fun execute(editor: Editor, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Boolean
  }

  final override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    // Here we have to save the last changed command. This should be done separately for each
    // call of the task, not for each caret. Currently there is no way to schedule any action
    // to be worked after each task. So here we override the deprecated execute function which
    // is called for each task and call the handlers for each caret, if implemented.

    editor.vimChangeActionSwitchMode = null

    val worked = Ref.create(true)
    when (this) {
      is ForEachCaret -> {
        editor.caretModel.runForEachCaret({ caret ->
          if (!caret.isValid) return@runForEachCaret
          if (!execute(editor, caret, context, cmd.count, cmd.rawCount, cmd.argument)) {
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
