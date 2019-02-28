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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.ex.*
import com.maddyhome.idea.vim.ex.CommandHandler.Flag.WRITABLE
import com.maddyhome.idea.vim.ex.CommandParser
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.commands
import com.maddyhome.idea.vim.ex.flags
import com.maddyhome.idea.vim.group.copy.PutCopyGroup
import com.maddyhome.idea.vim.handler.CaretOrder
import com.maddyhome.idea.vim.helper.EditorHelper

class CopyTextHandler : CommandHandler(
        commands("co[py]", "t"),
        flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_REQUIRED, WRITABLE)
) {
    override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
        val carets = EditorHelper.getOrderedCaretsList(editor, CaretOrder.DECREASING_OFFSET)
        for (caret in carets) {
            val range = cmd.getTextRange(editor, caret, context, false)
            val text = EditorHelper.getText(editor, range.startOffset, range.endOffset)

            val arg = CommandParser.getInstance().parse(cmd.argument)
            val line = arg.ranges.getFirstLine(editor, caret, context)
            val offset = VimPlugin.getMotion().moveCaretToLineStart(editor, line + 1)

            PutCopyGroup.putText(editor, caret, context, text, SelectionType.LINE_WISE, CommandState.SubMode.NONE,
                    offset, 1, true, false)
        }
        return true
    }
}
