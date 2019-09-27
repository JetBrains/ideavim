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

package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import java.util.*
import javax.swing.KeyStroke

/**
 * This represents a single Vim command to be executed. It may optionally include an argument if appropriate for
 * the command. The command has a count and a type.
 */
data class Command(
  var rawCount: Int,
  var action: EditorActionHandlerBase,
  val type: Type,
  var flags: EnumSet<CommandFlags>,
  var keys: List<KeyStroke>
) {

  init {
    action.process(this)
  }

  var count: Int
    get() = rawCount.coerceAtLeast(1)
    set(value) {
      rawCount = value
    }

  var argument: Argument? = null

  enum class Type {
    /**
     * Represents undefined commands.
     */
    UNDEFINED,
    /**
     * Represents commands that actually move the cursor and can be arguments to operators.
     */
    MOTION,
    /**
     * Represents commands that insert new text into the editor.
     */
    INSERT,
    /**
     * Represents commands that remove text from the editor.
     */
    DELETE,
    /**
     * Represents commands that change text in the editor.
     */
    CHANGE,
    /**
     * Represents commands that copy text in the editor.
     */
    COPY,
    PASTE,
    // TODO REMOVE?
    RESET,
    /**
     * Represents commands that select the register.
     */
    SELECT_REGISTER,
    OTHER_READONLY,
    OTHER_WRITABLE,
    /**
     * Represent commands that don't require an outer read or write action for synchronization.
     */
    OTHER_SELF_SYNCHRONIZED,
    COMPLETION;

    val isRead: Boolean
      get() = when (this) {
        MOTION, COPY, SELECT_REGISTER, OTHER_READONLY, COMPLETION -> true
        else -> false
      }

    val isWrite: Boolean
      get() = when (this) {
        INSERT, DELETE, CHANGE, PASTE, RESET, OTHER_WRITABLE -> true
        else -> false
      }
  }
}
