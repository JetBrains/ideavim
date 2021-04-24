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

package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.CommandHandlerFlags
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.flags
import com.maddyhome.idea.vim.ex.vimscript.VimScriptCommandHandler
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.key.ShortcutOwner
import com.maddyhome.idea.vim.key.ShortcutOwnerInfo

class SetKeyHandler  : CommandHandler.SingleExecution(), VimScriptCommandHandler {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
    execute(cmd)
    return true
  }

  override fun execute(cmd: ExCommand) {
    val args = cmd.argument.split(" ")
    val key = parseKeys(args[0]).single()
    val owner = ShortcutOwner.fromString(args[2])
    val existingInfo: ShortcutOwnerInfo = VimPlugin.getKey().savedShortcutConflicts[key]!!
    val newInfo = when (args[1]) {
      "i" -> existingInfo.copy(insert = owner)
      else -> error("")
    }
    VimPlugin.getKey().savedShortcutConflicts[key] = newInfo
  }
}
