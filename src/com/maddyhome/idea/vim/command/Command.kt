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

package com.maddyhome.idea.vim.command

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import java.util.*

/**
 * This represents a single Vim command to be executed (operator, motion, text object, etc.). It may optionally include
 * an argument if appropriate for the command. The command has a count and a type.
 */
data class Command(
  var rawCount: Int,
  var action: EditorActionHandlerBase,
  val type: Type,
  var flags: EnumSet<CommandFlags>
) {

  constructor(rawCount: Int, register: Char): this(rawCount, NonExecutableActionHandler, Type.SELECT_REGISTER, EnumSet.of(CommandFlags.FLAG_EXPECT_MORE)) {
    this.register = register
  }

  init {
    action.process(this)
  }

  var count: Int
    get() = rawCount.coerceAtLeast(1)
    set(value) {
      rawCount = value
    }

  var argument: Argument? = null
  var register: Char? = null

  fun isLinewiseMotion(): Boolean {
    return when (action) {
      is TextObjectActionHandler -> (action as TextObjectActionHandler).visualType == TextObjectVisualType.LINE_WISE
      is MotionActionHandler -> (action as MotionActionHandler).motionType == MotionType.LINE_WISE
      else -> error("Command is not a motion: $action")
    }
  }

  enum class Type {
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
    /**
     * Represents commands that select the register.
     */
    SELECT_REGISTER,
    OTHER_READONLY,
    OTHER_WRITABLE,
    /**
     * Represent commands that don't require an outer read or write action for synchronization.
     */
    OTHER_SELF_SYNCHRONIZED;

    val isRead: Boolean
      get() = when (this) {
        MOTION, COPY, OTHER_READONLY -> true
        else -> false
      }

    val isWrite: Boolean
      get() = when (this) {
        INSERT, DELETE, CHANGE, PASTE, OTHER_WRITABLE -> true
        else -> false
      }
  }
}

private object NonExecutableActionHandler : EditorActionHandlerBase(false) {
  override val type: Command.Type
    get() = error("This action should not be executed")

  override fun baseExecute(editor: Editor, caret: Caret, context: DataContext, cmd: Command): Boolean {
    error("This action should not be executed")
  }
}
