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

package com.maddyhome.idea.vim.ex

data class CommandName(val required: String, val optional: String = "")

fun commands(vararg commands: String) = commands.map { command ->
  val openBracketIndex = command.indexOf('[')
  if (openBracketIndex < 0) CommandName(command)
  else CommandName(command.take(openBracketIndex), command.substring(openBracketIndex + 1, command.lastIndex))
}.toTypedArray()

fun flags(
  rangeFlag: CommandHandler.RangeFlag,
  argumentFlag: CommandHandler.ArgumentFlag,
  access: CommandHandler.Access,
  vararg flags: CommandHandler.Flag
) = CommandHandlerFlags(rangeFlag, argumentFlag, access, flags.toSet())
