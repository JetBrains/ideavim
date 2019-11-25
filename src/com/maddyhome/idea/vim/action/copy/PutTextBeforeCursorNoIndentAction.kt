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
package com.maddyhome.idea.vim.action.copy

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.group.copy.PutData
import com.maddyhome.idea.vim.group.copy.PutData.TextData
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler

class PutTextBeforeCursorNoIndentAction : ChangeEditorActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override fun execute(editor: Editor,
                       context: DataContext,
                       count: Int,
                       rawCount: Int,
                       argument: Argument?): Boolean {
    val lastRegister = VimPlugin.getRegister().lastRegister
    val textData = if (lastRegister != null) TextData(lastRegister.text, lastRegister.type, lastRegister.transferableData) else null
    val putData = PutData(textData, null, count, insertTextBeforeCaret = true, _indent = false, caretAfterInsertedText = false, putToLine = -1)
    return VimPlugin.getPut().putText(editor, context, putData)
  }
}
