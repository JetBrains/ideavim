/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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
package com.maddyhome.idea.vim.regexp

class Flags {
  private var flags: Int

  constructor() {
    flags = 0
  }

  constructor(flags: Int) {
    this.flags = flags
  }

  fun get(): Int {
    return flags
  }

  fun isSet(flag: Int): Boolean {
    return flags and flag != 0
  }

  fun allSet(flags: Int): Boolean {
    return this.flags and flags == flags
  }

  fun init(flags: Int): Int {
    this.flags = flags
    return this.flags
  }

  fun set(flags: Int): Int {
    this.flags = this.flags or flags
    return this.flags
  }

  fun unset(flags: Int): Int {
    this.flags = this.flags and flags.inv()
    return this.flags
  }
}
