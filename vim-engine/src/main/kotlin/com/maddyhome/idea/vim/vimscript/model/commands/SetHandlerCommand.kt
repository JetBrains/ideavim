/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.key.ShortcutOwner
import com.maddyhome.idea.vim.key.ShortcutOwnerInfo
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

@ExCommand(command = "sethandler")
data class SetHandlerCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    return if (doCommand()) ExecutionResult.Success else ExecutionResult.Error
  }

  private fun doCommand(): Boolean {
    if (argument.isBlank()) return false

    val args = argument.trim().split(" ")
    if (args.isEmpty()) return false

    val key = try {
      val shortcut = args[0]
      if (shortcut.startsWith('<')) injector.parser.parseKeys(shortcut).first() else null
    } catch (_: IllegalArgumentException) {
      null
    }

    val owner = ShortcutOwnerInfo.allPerModeVim
    val skipShortcut = if (key == null) 0 else 1
    val resultingOwner = args.drop(skipShortcut).fold(owner) { currentOwner: ShortcutOwnerInfo.PerMode?, newData ->
      updateOwner(currentOwner, newData)
    } ?: return false

    if (key != null) {
      injector.keyGroup.savedShortcutConflicts[key] = resultingOwner
    } else {
      injector.keyGroup.shortcutConflicts.keys.forEach { conflictKey ->
        injector.keyGroup.savedShortcutConflicts[conflictKey] = resultingOwner
      }
    }
    return true
  }

  companion object {
    // todo you should make me internal
    fun updateOwner(owner: ShortcutOwnerInfo.PerMode?, newData: String): ShortcutOwnerInfo.PerMode? {
      if (owner == null) return null
      val split = newData.split(":", limit = 2)
      if (split.size != 2) return null

      val left = split[0]
      val right = ShortcutOwner.fromStringOrNull(split[1]) ?: return null

      var currentOwner: ShortcutOwnerInfo.PerMode = owner
      val modeSplit = left.split("-")
      modeSplit.forEach {
        currentOwner = when (it) {
          "n" -> currentOwner.copy(normal = right)
          "i" -> currentOwner.copy(insert = right)
          "v" -> currentOwner.copy(visual = right, select = right)
          "x" -> currentOwner.copy(visual = right)
          "s" -> currentOwner.copy(select = right)
          "a" -> currentOwner.copy(normal = right, insert = right, visual = right, select = right)
          else -> return null
        }
      }
      return currentOwner
    }
  }
}
