/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

import com.maddyhome.idea.vim.api.injector
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
interface Node<T> {
  val debugString: String
  val parent: Node<T>?

  val root: Node<T>
    get() = parent?.root ?: this
}

/** Represents a complete command */
data class CommandNode<T>(override val parent: Node<T>, val actionHolder: T, private val name: String) : Node<T> {
  override val debugString: String
    get() = toString()

  override fun toString() = "COMMAND NODE ($name - ${actionHolder.toString()})"
}

/** Represents a part of the command */
open class CommandPartNode<T>(
  override val parent: Node<T>?,
  internal val name: String,
  internal val depth: Int) : Node<T> {

  val children = mutableMapOf<KeyStroke, Node<T>>()

  operator fun set(stroke: KeyStroke, node: Node<T>) {
    children[stroke] = node
  }

  operator fun get(stroke: KeyStroke): Node<T>? = children[stroke]

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false
    return true
  }

  override fun hashCode() = super.hashCode()

  override fun toString() = "COMMAND PART NODE ($name - ${children.size} children)"

  override val debugString
    get() = buildString {
      append("COMMAND PART NODE(")
      appendLine(name)
      children.entries.forEach {
        repeat(depth + 1) { append(" ") }
        append(injector.parser.toKeyNotation(it.key))
        append(" - ")
        appendLine(it.value.debugString)
      }
      repeat(depth) { append(" ") }
      append(")")
    }
}

/** Represents a root node for the mode */
class RootNode<T>(name: String) : CommandPartNode<T>(null, name, 0) {
  override val debugString: String
    get() = "ROOT NODE ($name)\n" + super.debugString

  override fun toString() = "ROOT NODE ($name - ${children.size} children)"
}

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

  val childName = injector.parser.toKeyNotation(key)
  val name = when (base) {
    is RootNode -> base.name + "_" + childName
    else -> base.name + childName
  }
  val newNode: Node<T> = if (isLastInSequence) {
    CommandNode(base, actionHolder, name)
  } else {
    CommandPartNode(base, name, base.depth + 1)
  }
  base[key] = newNode
  return newNode
}
