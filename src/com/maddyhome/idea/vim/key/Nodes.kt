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

import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.helper.noneOfEnum
import java.util.*
import javax.swing.KeyStroke

/**
 * Marker interface for all key/action tree nodes
 */
interface Node

/**
 * This abstract node is used as a base for any node that can contain child nodes
 */
sealed class ParentNode : Node {

  protected val children: MutableMap<KeyStroke, Node> = mutableMapOf()

  /** This adds a child node keyed by the supplied key */
  fun addChild(child: Node, key: KeyStroke) {
    children[key] = child
  }

  /** Returns the child node associated with the supplied key. The key must be the same as used in [addChild] */
  fun getChild(key: KeyStroke): Node? = children[key]

  /**
   * Returns the child node associated with the supplied key. The key must be the same as used in [.addChild]
   * If this is BranchNode and no such child is found but there is an argument node, the argument node is returned.
   */
  open fun getChildOrArgument(key: KeyStroke): Node? = children[key]
}

class RootNode : ParentNode() {
  override fun toString(): String =
    "RootNode[children=[${children.entries.joinToString { (key, value) -> "$key -> $value" }}]"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    return true
  }

  override fun hashCode(): Int = javaClass.hashCode()
}

class ArgumentNode(
  val action: EditorActionHandlerBase,
  val cmdType: Command.Type,
  val argType: Argument.Type,
  val flags: EnumSet<CommandFlags>
) : Node {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ArgumentNode

    if (action != other.action) return false
    if (cmdType != other.cmdType) return false
    if (argType != other.argType) return false
    if (flags != other.flags) return false

    return true
  }

  override fun hashCode(): Int {
    var result = action.hashCode()
    result = 31 * result + cmdType.hashCode()
    result = 31 * result + argType.hashCode()
    result = 31 * result + flags.hashCode()
    return result
  }

  override fun toString(): String = "ArgumentNode(action=$action, cmdType=$cmdType, argType=$argType, flags=$flags)"
}

class BranchNode(
  val key: KeyStroke,
  flags: EnumSet<CommandFlags> = noneOfEnum()
) : ParentNode() {

  var argument: ArgumentNode? = null

  val flags: EnumSet<CommandFlags> = EnumSet.copyOf(flags)

  /**
   * Returns the child node associated with the supplied key. The key must be the same as used in [addChild].
   * If no such child is found but there is an argument node, the argument node is returned.
   */
  override fun getChildOrArgument(key: KeyStroke): Node? = getChild(key) ?: argument

  override fun toString(): String =
    "BranchNode[children=[${children.entries.joinToString { (key, value) -> "$key -> $value" }}]"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as BranchNode

    if (key != other.key) return false
    if (flags != other.flags) return false

    return true
  }

  override fun hashCode(): Int {
    var result = key.hashCode()
    result = 31 * result + flags.hashCode()
    return result
  }
}

class CommandNode(
  val key: KeyStroke,
  val action: EditorActionHandlerBase,
  val cmdType: Command.Type,
  val flags: EnumSet<CommandFlags>
) : Node {

  override fun toString(): String = "CommandNode[key=$key, action=$action, argType=$cmdType]"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as CommandNode

    @Suppress("DuplicatedCode")
    if (key != other.key) return false
    if (action != other.action) return false
    if (cmdType != other.cmdType) return false
    if (flags != other.flags) return false

    return true
  }

  override fun hashCode(): Int {
    var result = key.hashCode()
    result = 31 * result + action.hashCode()
    result = 31 * result + cmdType.hashCode()
    result = 31 * result + flags.hashCode()
    return result
  }
}
