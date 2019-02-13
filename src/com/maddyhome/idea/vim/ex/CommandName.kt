/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2016 The IdeaVim authors
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ex

data class CommandName(val required: String, val optional: String = "")

class CommandNameBuilder {
    val commands = hashSetOf<CommandName>()

    operator fun String.unaryPlus(): CommandName {
        val command = CommandName(this)
        commands.add(command)
        return command
    }

    operator fun CommandName.unaryPlus(): CommandName {
        commands.add(this)
        return this
    }

    infix fun CommandName.withOptional(optional: String): CommandName {
        val command = CommandName(this.required, optional)
        commands.remove(this)
        commands.add(command)
        return command
    }

    fun build() = commands.toTypedArray()
}

inline fun commands(addCommands: CommandNameBuilder.() -> Unit): Array<CommandName> {
    val commands = CommandNameBuilder()
    commands.addCommands()
    return commands.build()
}
