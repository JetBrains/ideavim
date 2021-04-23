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

import org.jetbrains.annotations.NonNls

/**
 * @author vlan
 */

data class ShortcutOwnerInfo(
  val normal: ShortcutOwner,
  val insert: ShortcutOwner,
  val visual: ShortcutOwner,
  val select: ShortcutOwner
) {
  companion object {
    @JvmStatic
    fun allOf(owner: ShortcutOwner): ShortcutOwnerInfo {
      return ShortcutOwnerInfo(owner, owner, owner, owner)
    }

    @JvmField
    val allUndefined = allOf(ShortcutOwner.UNDEFINED)
    val allVim = allOf(ShortcutOwner.VIM)
    val allIde = allOf(ShortcutOwner.IDE)
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
  }
}
