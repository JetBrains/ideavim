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

package com.maddyhome.idea.vim.key

import javax.swing.KeyStroke

/**
 * All the commands are stored into the tree where the key is either a complete command (for `x`, `j`, `l`, etc.),
 * or a part of a command (e.g. `g` for `gg`).
 *
 * This tree is pretty wide and looks like this:
 *
 *              root
 *               |
 *    -------------------------------------------
 *    |       |           |                     |
 *    j       G           g                     f
 *                        |                     |
 *                   ----------        ----------------
 *                   |        |         |      |       |
 *                   c        f         c      o       m
 *
 *
 *  Here j, G, c, f, c, o, m will be presented as a [CommandNode], and g and f as a [CommandPartNode]
 *
 *
 * If the command is complete, it's represented as a [CommandNode]. If this character is a part of command
 *   and the user should complete the sequence, it's [CommandPartNode]
 */
@Suppress("GrazieInspection")
interface Node<T>

/** Represents a complete command */
class CommandNode<T>(val actionHolder: T) : Node<T>

/** Represents a part of the command */
open class CommandPartNode<T> : Node<T>, HashMap<KeyStroke, Node<T>>()

/** Represents a root node for the mode */
class RootNode<T> : CommandPartNode<T>()

fun <T> Node<T>.addLeafs(keyStrokes: List<KeyStroke>, actionHolder: T) {
  var node: Node<T> = this
  val len = keyStrokes.size
  // Add a child for each keystroke in the shortcut for this action
  for (i in 0 until len) {
    if (node !is CommandPartNode<*>) {
      error("Error in tree constructing")
    }
    node = addNode(node as CommandPartNode<T>, actionHolder, keyStrokes[i], i == len - 1)
  }
}

private fun <T> addNode(base: CommandPartNode<T>, actionHolder: T, key: KeyStroke, isLastInSequence: Boolean): Node<T> {
  val existing = base[key]
  if (existing != null) return existing

  val newNode: Node<T> = if (isLastInSequence) CommandNode(actionHolder) else CommandPartNode()
  base[key] = newNode
  return newNode
}
