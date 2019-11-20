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

package com.maddyhome.idea.vim.key

import com.maddyhome.idea.vim.handler.ActionBeanClass
import javax.swing.KeyStroke

/**
 * All the commands are stored into the tree where the key is either a complete command (for `x`, `j`, `l`, etc.),
 * or a part of a command (e.g. `g` for `gg`).
 *
 * This tree is pretty wide and looks like this:
 *
 *              root
 *               |
 *    -----------------------------------
 *    |       |           |             |
 *    j       G           g             f
 *              ----------        ----------------
 *             |        |         |      |       |
 *             c        f         c      o       m
 *
 *
 *  Here j, G, c, f, c, o, m will be presented as a [CommandNode], and g and f as a [CommandPartNode]
 *
 *
 * If the command is complete, it's represented as a [CommandNode]. If this character is a part of command
 *   and the user should complete the sequence, it's [CommandPartNode]
 */
interface Node

/** Represents a complete command */
class CommandNode(val actionHolder: ActionBeanClass) : Node

/** Represents a part of the command */
open class CommandPartNode : Node, HashMap<KeyStroke, Node>()

/** Represents a root node for the mode */
class RootNode : CommandPartNode()
