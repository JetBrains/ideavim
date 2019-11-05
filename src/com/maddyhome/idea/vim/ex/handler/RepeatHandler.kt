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

package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.*
import com.maddyhome.idea.vim.ex.CommandHandler.Flag.DONT_SAVE_LAST
import com.maddyhome.idea.vim.group.MotionGroup

class RepeatHandler : CommandHandler.ForEachCaret() {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_REQUIRED, Access.SELF_SYNCHRONIZED, DONT_SAVE_LAST)

  private var lastArg = ':'

  @Throws(ExException::class)
  override fun execute(editor: Editor, caret: Caret, context: DataContext, cmd: ExCommand): Boolean {
    var arg = cmd.argument[0]
    if (arg == '@') arg = lastArg
    lastArg = arg

    val line = cmd.getLine(editor, caret, context)
    MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLine(editor, line, editor.caretModel.primaryCaret))

    if (arg == ':') {
      return CommandParser.getInstance().processLastCommand(editor, context, 1)
    }

    val reg = VimPlugin.getRegister().getPlaybackRegister(arg) ?: return false
    val text = reg.text ?: return false

    CommandParser.getInstance().processCommand(editor, context, text, 1)
    return true
  }
}
