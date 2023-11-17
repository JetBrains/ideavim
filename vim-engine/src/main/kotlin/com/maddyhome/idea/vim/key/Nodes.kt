/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

import javax.swing.KeyStroke

/**
 * COMPATIBILITY-LAYER: Moved from common package to this one
 * Please see: https://jb.gg/zo8n0r
 */

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
public interface Node<T>

/** Represents a complete command */
// Todo make T LazyVimCommand
public class CommandNode<T>(public val actionHolder: T) : Node<T>

/** Represents a part of the command */
public open class CommandPartNode<T> : Node<T>, HashMap<KeyStroke, Node<T>>()

/** Represents a root node for the mode */
public class RootNode<T> : CommandPartNode<T>()

public fun <T> Node<T>.addLeafs(keyStrokes: List<KeyStroke>, actionHolder: T) {
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
