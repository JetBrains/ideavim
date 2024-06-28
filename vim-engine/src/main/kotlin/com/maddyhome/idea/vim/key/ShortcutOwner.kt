/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.key

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.state.mode.Mode
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
      val owners = HashMap<ShortcutOwner, MutableList<String>>()
      owners[normal] = (owners[normal] ?: mutableListOf()).also { it.add("n") }
      owners[insert] = (owners[insert] ?: mutableListOf()).also { it.add("i") }
      owners[visual] = (owners[visual] ?: mutableListOf()).also { it.add("x") }
      owners[select] = (owners[select] ?: mutableListOf()).also { it.add("s") }

      if ("x" in (owners[ShortcutOwner.VIM] ?: emptyList()) && "s" in (owners[ShortcutOwner.VIM] ?: emptyList())) {
        val existing = owners[ShortcutOwner.VIM] ?: mutableListOf()
        existing.remove("x")
        existing.remove("s")
        existing.add("v")
        owners[ShortcutOwner.VIM] = existing
      }

      if ("x" in (owners[ShortcutOwner.IDE] ?: emptyList()) && "s" in (owners[ShortcutOwner.IDE] ?: emptyList())) {
        val existing = owners[ShortcutOwner.IDE] ?: mutableListOf()
        existing.remove("x")
        existing.remove("s")
        existing.add("v")
        owners[ShortcutOwner.IDE] = existing
      }

      if ((owners[ShortcutOwner.IDE] ?: emptyList()).isEmpty()) {
        owners.remove(ShortcutOwner.VIM)
        owners[ShortcutOwner.VIM] = mutableListOf("a")
      }

      if ((owners[ShortcutOwner.VIM] ?: emptyList()).isEmpty()) {
        owners.remove(ShortcutOwner.IDE)
        owners[ShortcutOwner.IDE] = mutableListOf("a")
      }

      val ideOwners = (owners[ShortcutOwner.IDE] ?: emptyList()).sortedBy { wights[it] ?: 1000 }.joinToString(separator = "-")
      val vimOwners = (owners[ShortcutOwner.VIM] ?: emptyList()).sortedBy { wights[it] ?: 1000 }.joinToString(separator = "-")

      return if (ideOwners.isNotEmpty() && vimOwners.isNotEmpty()) {
        ideOwners + ":" + ShortcutOwner.IDE.ownerName + " " + vimOwners + ":" + ShortcutOwner.VIM.ownerName
      } else if (ideOwners.isNotEmpty() && vimOwners.isEmpty()) {
        ideOwners + ":" + ShortcutOwner.IDE.ownerName
      } else if (ideOwners.isEmpty() && vimOwners.isNotEmpty()) {
        vimOwners + ":" + ShortcutOwner.VIM.ownerName
      } else {
        error("Unexpected state")
      }
    }
  }

  fun forEditor(editor: VimEditor): ShortcutOwner {
    return when (this) {
      is AllModes -> this.owner
      is PerMode -> when (editor.mode) {
        is Mode.NORMAL -> this.normal
        is Mode.VISUAL -> this.visual
        is Mode.SELECT -> this.visual
        Mode.INSERT -> this.insert
        is Mode.CMD_LINE -> this.normal
        is Mode.OP_PENDING -> this.normal
        Mode.REPLACE -> this.insert
      }
    }
  }

  companion object {
    @JvmField
    val allUndefined: AllModes = AllModes(ShortcutOwner.UNDEFINED)
    val allVim: AllModes = AllModes(ShortcutOwner.VIM)
    val allIde: AllModes = AllModes(ShortcutOwner.IDE)

    val allPerModeVim: PerMode = PerMode(ShortcutOwner.VIM, ShortcutOwner.VIM, ShortcutOwner.VIM, ShortcutOwner.VIM)
    val allPerModeIde: PerMode = PerMode(ShortcutOwner.IDE, ShortcutOwner.IDE, ShortcutOwner.IDE, ShortcutOwner.IDE)

    private val wights = mapOf(
      "a" to 0,
      "n" to 1,
      "i" to 2,
      "x" to 3,
      "s" to 4,
      "v" to 5,
    )
  }
}

enum class ShortcutOwner(val ownerName: @NonNls String, private val title: @NonNls String) {
  UNDEFINED("undefined", "Undefined"),
  IDE(Constants.IDE_STRING, "IDE"),
  VIM(Constants.VIM_STRING, "Vim"),
  ;

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
