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
package com.maddyhome.idea.vim.key

import com.google.common.collect.HashMultimap
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.mode
import org.jetbrains.annotations.NonNls

sealed class ShortcutOwnerInfo {
  data class AllModes(val owner: ShortcutOwner) : ShortcutOwnerInfo()

  data class PerMode(
    val normal: ShortcutOwner,
    val insert: ShortcutOwner,
    val visual: ShortcutOwner,
    val select: ShortcutOwner,
  ) : ShortcutOwnerInfo() {
    fun toNotation(): String {
      val owners = HashMultimap.create<ShortcutOwner, String>()
      owners.put(normal, "n")
      owners.put(insert, "i")
      owners.put(visual, "x")
      owners.put(select, "s")

      if ("x" in owners[ShortcutOwner.VIM] && "s" in owners[ShortcutOwner.VIM]) {
        owners.remove(ShortcutOwner.VIM, "x")
        owners.remove(ShortcutOwner.VIM, "s")
        owners.put(ShortcutOwner.VIM, "v")
      }

      if ("x" in owners[ShortcutOwner.IDE] && "s" in owners[ShortcutOwner.IDE]) {
        owners.remove(ShortcutOwner.IDE, "x")
        owners.remove(ShortcutOwner.IDE, "s")
        owners.put(ShortcutOwner.IDE, "v")
      }

      if (owners[ShortcutOwner.IDE].isEmpty()) {
        owners.removeAll(ShortcutOwner.VIM)
        owners.put(ShortcutOwner.VIM, "a")
      }

      if (owners[ShortcutOwner.VIM].isEmpty()) {
        owners.removeAll(ShortcutOwner.IDE)
        owners.put(ShortcutOwner.IDE, "a")
      }

      return owners[ShortcutOwner.IDE]
        .sortedBy { wights[it] ?: 1000 }
        .joinToString(separator = "-") +

        ":" + ShortcutOwner.IDE.ownerName + " " +

        owners[ShortcutOwner.VIM]
          .sortedBy { wights[it] ?: 1000 }
          .joinToString(separator = "-") +
        ":" + ShortcutOwner.VIM.ownerName
    }
  }

  fun forEditor(editor: Editor): ShortcutOwner {
    return when (this) {
      is AllModes -> this.owner
      is PerMode -> when (editor.mode) {
        CommandState.Mode.COMMAND -> this.normal
        CommandState.Mode.VISUAL -> this.visual
        CommandState.Mode.SELECT -> this.visual
        CommandState.Mode.INSERT -> this.insert
        CommandState.Mode.CMD_LINE -> this.normal
        CommandState.Mode.OP_PENDING -> this.normal
        CommandState.Mode.REPLACE -> this.insert
      }
    }
  }

  companion object {
    @JvmField
    val allUndefined = AllModes(ShortcutOwner.UNDEFINED)
    val allVim = AllModes(ShortcutOwner.VIM)
    val allIde = AllModes(ShortcutOwner.IDE)

    val allPerModeVim = PerMode(ShortcutOwner.VIM, ShortcutOwner.VIM, ShortcutOwner.VIM, ShortcutOwner.VIM)

    private val wights = mapOf(
      "a" to 0,
      "n" to 1,
      "i" to 2,
      "x" to 3,
      "s" to 4,
      "v" to 5
    )
  }
}

enum class ShortcutOwner(val ownerName: @NonNls String, private val title: @NonNls String) {
  UNDEFINED("undefined", "Undefined"),
  IDE(Constants.IDE_STRING, "IDE"),
  VIM(Constants.VIM_STRING, "Vim");

  override fun toString(): String = title

  private object Constants {
    const val IDE_STRING: @NonNls String = "ide"
    const val VIM_STRING: @NonNls String = "vim"
  }

  companion object {
    @JvmStatic
    fun fromString(s: String): ShortcutOwner = when (s) {
      Constants.IDE_STRING -> IDE
      Constants.VIM_STRING -> VIM
      else -> UNDEFINED
    }

    fun fromStringOrNull(s: String): ShortcutOwner? {
      return when {
        Constants.IDE_STRING.equals(s, ignoreCase = true) -> IDE
        Constants.VIM_STRING.equals(s, ignoreCase = true) -> VIM
        else -> null
      }
    }
  }
}
