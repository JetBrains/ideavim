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

class SetKeyHandler : CommandHandler.SingleExecution(), VimScriptCommandHandler {
  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
    return doCommand(cmd)
  }

  override fun execute(cmd: ExCommand) {
    doCommand(cmd)
  }

  private fun doCommand(cmd: ExCommand): Boolean {
    if (cmd.argument.isBlank()) return false

    val args = cmd.argument.split(" ")
    if (args.isEmpty()) return false

    val key = try {
      parseKeys(args[0]).first()
    } catch (e: IllegalArgumentException) {
      null
    }

    val owner = ShortcutOwnerInfo.allPerModeVim

    val resultingOwner = args.drop(1).fold(owner) { currentOwner: ShortcutOwnerInfo.PerMode?, newData ->
      updateOwner(currentOwner, newData)
    } ?: return false

    if (key != null) {
      VimPlugin.getKey().savedShortcutConflicts[key] = resultingOwner
    } else {
      val conflicts = VimPlugin.getKey().savedShortcutConflicts
      conflicts.keys.forEach { conflictKey ->
        conflicts[conflictKey] = resultingOwner
      }
    }
    return true
  }

  private fun updateOwner(owner: ShortcutOwnerInfo.PerMode?, newData: String): ShortcutOwnerInfo.PerMode? {
    if (owner == null) return null
    val split = newData.split(":", limit = 2)
    if (split.size != 2) return null

    val left = split[0]
    val right = ShortcutOwner.fromStringOrVim(split[1])

    var currentOwner: ShortcutOwnerInfo.PerMode = owner
    val modeSplit = left.split("-")
    modeSplit.forEach {
      currentOwner = when (it) {
        "n" -> currentOwner.copy(normal = right)
        "i" -> currentOwner.copy(insert = right)
        "v" -> currentOwner.copy(visual = right, select = right)
        "x" -> currentOwner.copy(visual = right)
        else -> return null
      }
    }
    return currentOwner
  }
}
