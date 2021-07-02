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

package com.maddyhome.idea.vim.option

import com.maddyhome.idea.vim.helper.VimNlsSafe
import java.util.regex.Pattern

/**
 * Creates the option
 *
 * @param name    The name of the option
 * @param abbrev  The short name
 * @param defaultValues    The option's default values
 * @param pattern A regular expression that is used to validate new values. null if no check needed
 */
open class StringListOption @JvmOverloads constructor(
  @VimNlsSafe name: String,
  @VimNlsSafe abbrev: String,
  @VimNlsSafe defaultValues: Array<String>,
  @VimNlsSafe protected val pattern: String? = null
) :
  ListOption<String>(name, abbrev, defaultValues) {

  companion object {
    val empty = StringListOption("", "", emptyArray())
  }

  override fun convertToken(token: String): String? {
    if (pattern == null) {
      return token
    }
    if (Pattern.matches(pattern, token)) {
      return token
    }
    return null
  }
}
