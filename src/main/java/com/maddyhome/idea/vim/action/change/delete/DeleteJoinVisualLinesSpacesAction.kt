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
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import java.util.*

/**
 * @author vlan
 */
class DeleteJoinVisualLinesSpacesAction : VisualOperatorActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.DELETE

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_EXIT_VISUAL)

  override fun executeForAllCarets(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    caretsAndSelections: Map<VimCaret, VimSelection>,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if ((editor as IjVimEditor).editor.isOneLineMode) return false
    if (VimPlugin.getOptionService().isSet(OptionScope.LOCAL(editor), OptionConstants.ideajoinName)) {
      VimPlugin.getChange().joinViaIdeaBySelections(editor, context, caretsAndSelections)
      return true
    }
    val res = Ref.create(true)
    editor.editor.caretModel.runForEachCaret(
      { caret: Caret ->
        if (!caret.isValid) return@runForEachCaret
        val range = caretsAndSelections[IjVimCaret(caret)] ?: return@runForEachCaret
        if (!VimPlugin.getChange().deleteJoinRange(editor, IjVimCaret(caret), range.toVimTextRange(true).normalize(), true)) {
          res.set(false)
        }
      },
      true
    )
    return res.get()
  }
}
