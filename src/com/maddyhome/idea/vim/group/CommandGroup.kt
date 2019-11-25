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

package com.maddyhome.idea.vim.group

import com.maddyhome.idea.vim.common.Alias

/**
 * @author Elliot Courant
 */
class CommandGroup {
  companion object {
    val BLACKLISTED_ALIASES = arrayOf("X", "Next", "Print")
    private const val overridePrefix = "!"
  }

  private var aliases = HashMap<String, Alias>()

  fun isAlias(command: String): Boolean {
    val name = this.getAliasName(command)
    // If the first letter is not uppercase then it cannot be an alias
    // and reject immediately.
    if (name.isEmpty() || !name[0].isUpperCase()) {
      return false
    }

    // If the input is blacklisted, then it is not an alias.
    if (name in BLACKLISTED_ALIASES) {
      return false
    }

    return this.hasAlias(name)
  }

  fun hasAlias(name: String): Boolean {
    return name in this.aliases
  }

  fun getAlias(name: String): Alias {
    return this.aliases[name]!!
  }

  fun getAliasCommand(command: String, count: Int): String {
    return this.getAlias(this.getAliasName(command)).getCommand(command, count)
  }

  fun setAlias(name: String, alias: Alias) {
    this.aliases[name] = alias
  }

  fun removeAlias(name: String) {
    this.aliases.remove(name)
  }

  fun listAliases(): Set<Map.Entry<String, Alias>> {
    return this.aliases.entries
  }

  fun resetAliases() {
    this.aliases.clear()
  }

  private fun getAliasName(command: String): String {
    val items = command.split(" ")
    if (items.count() > 1) {
      return items[0].removeSuffix(overridePrefix)
    }
    return command.removeSuffix(overridePrefix)
  }
}
