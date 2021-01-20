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

import javax.swing.KeyStroke

class RequiredShortcut(val keyStroke: KeyStroke, val owner: MappingOwner)

sealed class MappingOwner {
  object IdeaVim : MappingOwner()

  @Suppress("DataClassPrivateConstructor")
  data class Plugin private constructor(val name: String) : MappingOwner() {
    companion object {
      fun get(name: String): Plugin = allOwners.computeIfAbsent(name) { Plugin(it) }

      fun remove(name: String) = allOwners.remove(name)

      private val allOwners: MutableMap<String, Plugin> = mutableMapOf()
    }
  }
}
