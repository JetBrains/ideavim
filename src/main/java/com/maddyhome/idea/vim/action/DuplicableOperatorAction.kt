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

package com.maddyhome.idea.vim.action

/**
 * There are some double-character commands like `cc`, `dd`, `yy`.
 * During the execution these commands are replaced with `c_`, `d_`, `y_`, etc.
 *
 * This is not any kind of workaround, this is exactly how the original vim works.
 *   The `dd` command (and others) should not be processed as a monolith command, or it will lead to problems
 *   like this: https://youtrack.jetbrains.com/issue/VIM-1189
 *
 * If some command implements this interface, and the user enters motion operator that is the same as the command itself, the
 *   motion operator will be replaced with `_`.
 */
interface DuplicableOperatorAction {
  val duplicateWith: Char
}
