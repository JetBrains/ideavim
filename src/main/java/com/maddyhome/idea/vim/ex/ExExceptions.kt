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
package com.maddyhome.idea.vim.ex

import com.maddyhome.idea.vim.helper.MessageHelper
import org.jetbrains.annotations.PropertyKey

class InvalidCommandException(message: String, cmd: String?) : ExException(message + if (cmd != null) " | $cmd" else "")

class InvalidRangeException(s: String) : ExException(s)

class MissingArgumentException : ExException()

class MissingRangeException : ExException()

class NoArgumentAllowedException : ExException()

class NoRangeAllowedException : ExException()

class FinishException : ExException()

fun exExceptionMessage(@PropertyKey(resourceBundle = MessageHelper.BUNDLE) code: String, vararg params: Any) =
  ExException(MessageHelper.message(code, *params)).apply { this.code = code }
