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
package com.maddyhome.idea.vim.action.change.delete

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope

class DeleteJoinLinesSpacesAction : ChangeEditorActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.DELETE

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if ((editor as IjVimEditor).editor.isOneLineMode) return false
    if (VimPlugin.getOptionService().isSet(OptionScope.LOCAL(editor), OptionConstants.ideajoinName)) {
      return VimPlugin.getChange().joinViaIdeaByCount(editor, context, operatorArguments.count1)
    }
    VimPlugin.getEditor().notifyIdeaJoin(editor.editor.project)
    val res = Ref.create(true)
    editor.editor.caretModel.runForEachCaret(
      { caret: Caret ->
        if (!VimPlugin.getChange().deleteJoinLines(editor, IjVimCaret(caret), operatorArguments.count1, true)) res.set(false)
      },
      true
    )
    return res.get()
  }
}
