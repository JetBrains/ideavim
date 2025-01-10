/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

import com.maddyhome.idea.vim.api.VimKeyGroup
import com.maddyhome.idea.vim.api.injector
import javax.swing.KeyStroke

/**
 * COMPATIBILITY-LAYER: Moved from common package to this one
 * Please see: https://jb.gg/zo8n0r
 *
 * Used by idea-which-key (latest is currently 0.10.3)
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
@Deprecated("Use KeyStrokeTrie and VimKeyGroup.getBuiltinCommandsTrie instead")
interface Node<T>

/** Represents a complete command */
@Suppress("DEPRECATION")
@Deprecated("Use KeyStrokeTrie and VimKeyGroup.getBuiltinCommandsTrie instead")
data class CommandNode<T>(val actionHolder: T) : Node<T> {
  override fun toString(): String {
    return "COMMAND NODE (${actionHolder.toString()})"
  }
}

/**
 * Represents a part of the command
 *
 * Vim-which-key uses this to get a map of all builtin Vim actions. Sadly, there is on Vim equivalent, so we can't
 * provide a Vim script function as an API. After retrieving with [VimKeyGroup.getKeyRoot], the node is iterated
 */
@Suppress("DEPRECATION")
@Deprecated("Use KeyStrokeTrie and VimKeyGroup.getBuiltinCommandsTrie instead")
open class CommandPartNode<T> internal constructor(private val trieNode: KeyStrokeTrie.TrieNode<T>) : Node<T>,
  AbstractMap<KeyStroke, Node<T>>() {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false
    return true
  }

  override fun hashCode() = super.hashCode()

  override fun toString(): String {
    return """
      COMMAND PART NODE(
      ${entries.joinToString(separator = "\n") { "    " + injector.parser.toKeyNotation(it.key) + " - " + it.value }}
      )
      """.trimIndent()
  }

  override val entries: Set<Map.Entry<KeyStroke, Node<T>>>
    get() {
      return buildMap {
        trieNode.visit { key, value ->
          val node: Node<T> = if (value.data == null) {
            CommandPartNode<T>(value)
          } else {
            CommandNode(value.data!!)
          }
          put(key, node)
        }
      }.entries
    }
}

@Suppress("DEPRECATION")
internal class RootNode<T>(trieNode: KeyStrokeTrie<T>) : CommandPartNode<T>(trieNode.getTrieNode(emptyList())!!)
